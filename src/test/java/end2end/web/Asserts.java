package end2end.web;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class Asserts {

    public static String joinAllHeadersFor(String key, 
            ResponseEntity<?> response) {
        return response.getHeaders().get(key)
                                    .stream()
                                    .collect(joining());
    }
    
    public static void assert200(ResponseEntity<?> response) {
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
    }
    
    public static void assert204(ResponseEntity<?> response) {
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getHeaders().getContentLength(), is(0L));
        assertNull(response.getBody());
    }
    
    public static void assert404(ResponseEntity<?> response) {
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }
    
    public static void assertNoCaching(ResponseEntity<?> response) {
        HttpHeaders hs = response.getHeaders();
        String cache = joinAllHeadersFor(HttpHeaders.CACHE_CONTROL, response);
        assertThat(cache, containsString("no-cache"));
        assertThat(cache, containsString("no-store"));
        assertThat(hs.getPragma(), containsString("no-cache"));
        assertThat(hs.getFirst("Expires"), is("0"));
    }
    
    public static void assertCacheForAsLongAsPossible(
            ResponseEntity<?> response) {
        String cache = joinAllHeadersFor(HttpHeaders.CACHE_CONTROL, response);
        assertThat(cache, containsString("max-age=31536000"));
    }
    
    public static void assertPlainText(ResponseEntity<?> response) {
        HttpHeaders hs = response.getHeaders();
        MediaType expected = new MediaType("text", "plain", 
                                           StandardCharsets.UTF_8);
        assertThat(hs.getContentType(), is(expected));
    }
    
}
