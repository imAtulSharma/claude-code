package tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;
import model.ParameterProperty;

import java.io.*;

public class BashTool {
    public static ChatCompletionTool getInstance() {
        return ChatCompletionTool.builder()
                .function(buildFunctionDefinition())
                .build();
    }

    private static FunctionDefinition buildFunctionDefinition() {
        return FunctionDefinition.builder()
                .name("Bash")
                .description("Execute a shell command")
                .parameters(buildFunctionParameters())
                .build();
    }

    private static FunctionParameters buildFunctionParameters() {
        ParameterProperty property = new ParameterProperty("string", "The command to execute");
        return FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(
                        java.util.Map.of("command", property)
                ))
                .putAdditionalProperty("required", JsonValue.from(
                        java.util.List.of("command")
                ))
                .build();
    }

    public static String execute(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});

        try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String output = stdout.lines().collect(java.util.stream.Collectors.joining("\n"));
            String error = stderr.lines().collect(java.util.stream.Collectors.joining("\n"));

            int exitCode = process.waitFor();

            if (!error.isEmpty()) {
                return "STDERR: " + error + (output.isEmpty() ? "" : "\nSTDOUT: " + output);
            }
            return output;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command interrupted: " + command, e);
        }
    }
}
