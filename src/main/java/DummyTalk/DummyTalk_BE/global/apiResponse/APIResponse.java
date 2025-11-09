package DummyTalk.DummyTalk_BE.global.apiResponse;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder ({"isSuccess", "code", "message", "result"})
public class APIResponse<T> {

    @JsonProperty
    private boolean success;

    private String code;

    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    /*public static <T> APIResponse<T> onSuccess(T result, SuccessCode status) {
        return new APIResponse<>(true, status.getCode(), status.getMessage(), result);
    }*/

    public static <T> APIResponse<T> onFailure(String code, String message, T data) {
        return new APIResponse<>(false, code, message, data);
    }

}

