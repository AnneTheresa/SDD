# Contract: MCP Tool getGicRates

## Provider

- Tool hosted by renamed MCP server module (currently mcp-test-server baseline).

## Tool Name

- getGicRates

## Input

- No required arguments.

## Output

- Plain-text payload readable by model.
- One line per GIC term with annual rate.
- Includes all supported terms:
  - SIX_MONTHS
  - ONE_YEAR
  - TWO_YEARS
  - THREE_YEARS
  - FIVE_YEARS

Example output style:
- SIX_MONTHS: 3.00%
- ONE_YEAR: 5.00%
- TWO_YEARS: 5.50%
- THREE_YEARS: 6.00%
- FIVE_YEARS: 7.00%

## Error Contract

- On connectivity/tool failure, error is surfaced to model via Spring AI tool error flow.
- Chat request must not fail solely because this tool is unreachable.

## Constraints

- Values are mocked/static in MCP module.
- No shared dependency on backend GicTerm enum.
