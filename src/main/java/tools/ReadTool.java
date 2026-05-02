package tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;
import model.ParameterProperty;

import java.io.*;

public class ReadTool {
    public static ChatCompletionTool getInstance() {
        return ChatCompletionTool.builder()
                .function(buildFunctionDefinition())
                .build();
    }

    private static FunctionDefinition buildFunctionDefinition() {
        return FunctionDefinition.builder()
                .name("Read")
                .description("Read and return the contents of a file")
                .parameters(buildFunctionParameters())
                .build();
    }

    private static FunctionParameters buildFunctionParameters() {
        ParameterProperty property = new ParameterProperty("string", "The path to the file to read");
        return FunctionParameters.builder()
                .putAdditionalProperty("file_path", JsonValue.from(property))
                .build();
    }

    public static String execute(String path) throws IOException {
        File file = new File(path);
        try (FileReader reader = new FileReader(file)) {
            return reader.readAllAsString();
        }
    }
}
