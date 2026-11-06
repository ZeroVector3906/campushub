package cn.campushub;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status=status; }
    public static ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST,message); }
    public static ApiException missing() { return new ApiException(HttpStatus.NOT_FOUND,"记录不存在"); }
    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN,"无权操作此记录"); }
}
@RestControllerAdvice
class ErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var error=e.getBindingResult().getFieldErrors().getFirst();
        var labels=Map.ofEntries(Map.entry("username","用户名"),Map.entry("password","密码"),Map.entry("name","名称"),Map.entry("email","邮箱"),Map.entry("studentNo","学号"),Map.entry("department","院系"),Map.entry("reason","申请理由"),Map.entry("title","标题"),Map.entry("description","详细说明"),Map.entry("location","地点"),Map.entry("quantity","数量"),Map.entry("dueDate","归还日期"),Map.entry("checkinCode","签到码"));
        String message=error.getDefaultMessage();
        if(message==null || !message.matches(".*[\\u4e00-\\u9fa5].*"))message="该项必填或格式不正确";
        return ResponseEntity.badRequest().body(Map.of("message","请检查"+labels.getOrDefault(error.getField(),"输入内容")+"："+message));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() { return ResponseEntity.status(409).body(Map.of("message","记录已存在或数据冲突，请刷新后重试")); }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> malformed() { return ResponseEntity.badRequest().body(Map.of("message","请求格式不正确，请检查日期和必填项")); }
}
