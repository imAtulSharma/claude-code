package tools;

import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionTool;
import model.ParameterProperty;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WriteTool {
    public static ChatCompletionTool getInstance() {
        return ChatCompletionTool.builder()
                .function(buildFunctionDefinition())
                .build();
    }

    private static FunctionDefinition buildFunctionDefinition() {
        return FunctionDefinition.builder()
                .name("Write")
                .description("Write content to a file")
                .parameters(buildFunctionParameters())
                .build();
    }

    private static FunctionParameters buildFunctionParameters() {
        ParameterProperty property1 = new ParameterProperty("string", "The path of the file to write to");
        ParameterProperty property2 = new ParameterProperty("string", "The content to write to the file");
        return FunctionParameters.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(
                        java.util.Map.of(
                                "file_path", property1,
                                "content", property2
                        )
                ))
                .putAdditionalProperty("required", JsonValue.from(
                        java.util.List.of("file_path", "content")
                ))
                .build();
    }

    public static void execute(String path, String content) throws IOException {
        File file = new File(path);
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
