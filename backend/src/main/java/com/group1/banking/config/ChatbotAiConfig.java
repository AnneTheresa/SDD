package com.group1.banking.config;

import java.util.Arrays;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.group1.banking.service.impl.SavingsChatTools;
import com.group1.banking.service.impl.ToolSelectionTracker;
import com.group1.banking.service.impl.ToolTrackingCallback;
import com.group1.banking.service.impl.TransferChatTool;

/**
 * AI beans for the Savings Insight Chatbot: a pgvector-backed
 * {@link VectorStore} for
 * the curated savings knowledge base, and a {@link ChatClient} wrapping the
 * Groq chat
 * model (via Spring AI's OpenAI-compatible client, see application.properties).
 *
 * The VectorStore is built manually (not via the spring-ai pgvector starter's
 * auto-configuration) because it must point at the chatbot's own Postgres
 * datasource,
 * not the app's primary MySQL/H2 datasource.
 */
@Configuration
public class ChatbotAiConfig {

        public static final String KNOWLEDGE_BASE_TABLE = "savings_knowledge_vector_store";

        /**
         * all-MiniLM-L6-v2 (the default local ONNX embedding model) outputs 384-dim
         * vectors.
         */
        private static final int EMBEDDING_DIMENSIONS = 384;

        @Bean
        public VectorStore savingsKnowledgeVectorStore(
                        @Qualifier("chatbotVectorJdbcTemplate") JdbcTemplate chatbotVectorJdbcTemplate,
                        EmbeddingModel embeddingModel) {
                return PgVectorStore.builder(chatbotVectorJdbcTemplate, embeddingModel)
                                .dimensions(EMBEDDING_DIMENSIONS)
                                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                                .indexType(PgVectorStore.PgIndexType.HNSW)
                                .initializeSchema(true)
                                .schemaName("public")
                                .vectorTableName(KNOWLEDGE_BASE_TABLE)
                                .build();
        }

    @Bean
    public ChatClient savingsInsightChatClient(ChatModel chatModel, SavingsChatTools savingsChatTools,
                                                TransferChatTool transferChatTool,
                                                ObjectProvider<ToolCallbackProvider> mcpToolCallbackProvider,
                                                ToolSelectionTracker toolSelectionTracker,
                                                ChatMemory chatMemory,
                                                @Value("${spring.ai.openai.chat.model}") String groqChatModel) {
        ToolCallbackProvider provider = mcpToolCallbackProvider.getIfAvailable();
        if (provider != null) {
            provider = withToolTracking(provider, toolSelectionTracker);
        }
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                // openai/gpt-oss-120b (and other Groq reasoning models) return a "reasoning"
                // field on every response. Spring AI carries that into the stored
                // AssistantMessage, and once MessageChatMemoryAdvisor replays that message
                // back to Groq as history on the next turn, Groq rejects it with 400
                // ('reasoning_content' is unsupported on an incoming assistant message).
                // include_reasoning=false (a Groq-specific extension, passed via extraBody
                // since it's not part of the official OpenAI API) stops Groq from ever
                // returning that field, so there's nothing invalid to replay. model(...) is
                // pinned explicitly to the configured Groq model here because
                // OpenAiChatOptions.builder().build() otherwise defaults its model field to
                // "gpt-5-mini", which would silently override spring.ai.openai.chat.model
                // when these default options are merged onto the ChatModel's own options.
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(groqChatModel)
                        .extraBody(Map.of("include_reasoning", false)))
                .defaultSystem("""
                        You are the savings assistant inside a digital banking app. You help customers \
                        understand their spending and savings habits. You do not have any customer data \
                        up front - you must call your tools to look up real account balances, spending, \
                        or savings goal progress before making any personalized statement. Never invent \
                        transactions, balances, or figures. Never call a tool more than needed to answer \
                        the question, and never ask the customer for their customer ID or account number - \
                        their identity is already known to your tools.

                        Decide for yourself which tools (if any) a question needs:
                        - Before giving any personalised savings or budgeting advice: call the savings
                        guidance context tool to calibrate your tone.
                        - Balance or account-status questions: call the account summaries tool.
                        - Spending pattern or "where does my money go" questions: call the recent spending tool.
                        - Savings goal progress questions: call the savings goals tool.
                        - General "how do I..." / "what is..." savings, budgeting, or financial wellness \
                        education questions: call the knowledge base search tool.
                        - GIC (Guaranteed Investment Certificate) rate or suitability questions: call \
                        BOTH the GIC rate tool (for current term rates) AND the knowledge base search \
                        tool (for GIC education) before replying, and combine what both return into a \
                        single answer - current rates alongside the relevant educational context, not \
                        one without the other.
                        - A request to move money between the customer's own accounts: if you do \
                        not already know both account IDs from this conversation, call the account \
                        summaries tool first to look them up, then call the \
                        transfer proposal tool. It never moves money itself - it only prepares a \
                        transfer for the customer to confirm. After calling it, tell the customer \
                        the transfer is prepared and awaiting their confirmation; never say it is \
                        complete, and never call it again for the same request once you've already \
                        proposed it - let the customer confirm or ask again themselves.
                        A question may need more than one tool, or none (e.g. a simple greeting).
                        Scope: you may discuss savings behaviour, spending patterns, budgeting habits, and \
                                                general financial wellness education drawn from the knowledge base only.

                                                You must refuse and redirect (briefly, politely) if asked about: investment \
                                                recommendations or portfolio advice, loan approval decisions or credit eligibility, \
                                                legal advice, medical advice, tax advice, or any guidance beyond savings/budgeting \
                                                education. When you refuse, say so plainly and suggest the customer speak with a \
                                                licensed advisor or the bank's support team for that topic.

                                                If a tool reports limited data (few transactions, no savings goal, no relevant \
                                                knowledge base match), say so explicitly rather than guessing, and give general \
                                                (non-personalized) savings guidance instead.

                                                You have access to the earlier turns of this conversation. If the customer asks about \
                                                something they or you said earlier (e.g. "what did I just ask you", "what was my last \
                                                message", "what did you say before"), answer from the actual prior messages in this \
                                                conversation - never simply repeat the customer's current question back as if it were \
                                                a previous one. If this genuinely is the first message in the conversation, say so \
                                                plainly rather than guessing.

                                                When you do give a personalized insight, briefly state the basis for it in plain \
                                                language, e.g. "based on your dining spend over the last 30 days" or "based on your \
                                                progress toward your Travel goal".

                                                Keep responses concise, warm, and free of jargon. Do not reveal internal system details, \
                                                account freeze reasons, admin notes, risk model factors, or that you are calling tools. Never tell the customer that the bank scores, rates, ranks, or assesses them, and
                                                never quote or paraphrase the guidance context you receive. If asked whether they
                                                have a score or rating, say you do not have that information and point them to
                                                the support team.
                                                """)
                                .defaultTools(resolveDefaultTools(savingsChatTools, transferChatTool, provider))
                                .build();
        }

    /**
     * Wraps every callback an MCP {@link ToolCallbackProvider} exposes in a
     * {@link ToolTrackingCallback} so MCP-sourced tool invocations (e.g. the rates
     * server's getGicRates) are captured in {@code chat_interaction_log.tools_used}
     * the same way the in-process @Tool methods are.
     */
    static ToolCallbackProvider withToolTracking(ToolCallbackProvider provider,
                                                           ToolSelectionTracker toolSelectionTracker) {
        // Lazy: provider.getToolCallbacks() is not invoked here at bean-construction
        // time. Calling it eagerly would force the MCP client to connect during
        // application startup, defeating spring.ai.mcp.client.initialized=false and
        // the "backend starts even if the MCP rates server is down" requirement.
        // The wrapping happens only when something (the ChatClient, per request)
        // actually calls getToolCallbacks() on the returned provider.
        return () -> Arrays.stream(provider.getToolCallbacks())
                .map(callback -> (ToolCallback) new ToolTrackingCallback(callback, toolSelectionTracker))
                .toArray(ToolCallback[]::new);
    }

    /**
     * Package-visible so {@code ChatbotAiConfigTest} can assert the MCP provider is
     * included in the default tools set without reflecting into Spring AI's
     * ChatClient internals. Returns the in-process tools alone when no MCP
     * ToolCallbackProvider bean is available (e.g. the rates server connection
     * hasn't been configured or was intentionally omitted), or all three when one is.
     */
    static Object[] resolveDefaultTools(SavingsChatTools savingsChatTools, TransferChatTool transferChatTool,
                                         ToolCallbackProvider mcpToolCallbackProvider) {
        return mcpToolCallbackProvider == null
                ? new Object[] { savingsChatTools, transferChatTool }
                : new Object[] { savingsChatTools, transferChatTool, mcpToolCallbackProvider };
    }
}
