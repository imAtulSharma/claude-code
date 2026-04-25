import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonObject;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import tools.ReadTool;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        String apiKey = System.getenv("OPENROUTER_API_KEY");
        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OPENROUTER_API_KEY is not set");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        ChatCompletion response = client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model("anthropic/claude-haiku-4.5")
                        .addUserMessage(prompt)
                        .addTool(ReadTool.getInstance())
                        .build()
        );

        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

        ChatCompletionMessage message = response.choices().getFirst().message();

        String content = message.content().orElse(null);
        List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(null);

        if(content != null)
            System.out.print(message.content().orElse(""));
        else if(toolCalls != null && !toolCalls.isEmpty())
            identifyToolsAndExecute(toolCalls);
    }

    private static void identifyToolsAndExecute(List<ChatCompletionMessageToolCall> toolCalls) {
        for (ChatCompletionMessageToolCall toolCall: toolCalls) {
            try {
                if (toolCall._type().equals(JsonValue.from("function"))) {
                    ChatCompletionMessageToolCall.Function function = toolCall.function();
                    String arguments = function.arguments();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(arguments);

                    if (function.name().equals("Read")) {
                        String filePath = node.get("parameter").asText();
                        ReadTool.execute(filePath);
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
