import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.*;
import helper.OpenAIHelper;
import tools.ReadTool;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final List<ChatCompletionMessageParam> messages = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 2 || !"-p".equals(args[0])) {
            System.err.println("Usage: program -p <prompt>");
            System.exit(1);
        }

        String prompt = args[1];

        // You can use print statements as follows for debugging, they'll be visible when running tests.
        System.err.println("Logs from your program will appear here!");

        OpenAIClient client = OpenAIHelper.getClient();

        messages.add(
                ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
                )
        );

        ChatCompletion response = OpenAIHelper.chat(client, messages);

        handleResponse(client, response, prompt);
    }

    private static void handleResponse(OpenAIClient client, ChatCompletion response, String prompt) {
        if (response.choices().isEmpty()) {
            throw new RuntimeException("no choices in response");
        }

        ChatCompletionMessage message = response.choices().getFirst().message();
        List<ChatCompletionMessageToolCall> toolCalls = message.toolCalls().orElse(new ArrayList<>());

        messages.add(
                ChatCompletionMessageParam.ofAssistant(
                        ChatCompletionAssistantMessageParam.builder()
                                .toolCalls(toolCalls)
                                .content(message.content().orElse(""))
                                .build()
                )
        );

        if(toolCalls.isEmpty()) {
            System.out.print(message.content().orElse(""));
            return;
        }

        identifyToolsAndExecute(toolCalls);

        ChatCompletion newResponse = OpenAIHelper.chat(client, messages);
        handleResponse(client, newResponse, prompt);
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
                        String fileContent = ReadTool.execute(filePath);
                        ChatCompletionToolMessageParam toolMessageParam = ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCall.id())
                                .content(fileContent)
                                .build();
                        messages.add(ChatCompletionMessageParam.ofTool(toolMessageParam));
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
