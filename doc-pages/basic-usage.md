### Basic usage

#### Simple example

Imagine that you develop WEB application and want to limit user to access for application no often then 10 times for second:

```java
import com.github.bucket4j.Bucket4j;

public class ThrottlingFilter implements javax.servlet.Filter {
    
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpSession session = httpRequest.getSession(true);

        Bucket bucket = (Bucket) session.getAttribute("throttler");
        if (bucket == null) {
            // build bucket with required capacity and associate it with particular user
            bucket = Bucket4j.builder()
                .withLimitedBandwidth(10, Duration.ofSeconds(1))
                .build();
            session.setAttribute("throttler", bucket);
        }

        // tryConsumeSingleToken returns false immediately if no tokens available with the bucket
        if (bucket.tryConsumeSingleToken()) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
            httpResponse.setContentType("text/plain");
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }
    }

}
```
 

#### Yet another simple example

Suppose you have a piece of code that polls a website and you would only like to be able to access the site 100 time per minute: 

```java
import com.github.bucket4j.Bucket4j;

// Create a token bucket with required capacity.
Bucket bucket = Bucket4j.builder()
                .withLimitedBandwidth(100, Duration.ofMinutes(1))
                .build();

// ...
while (true) {
  // Consume a token from the token bucket.  
  // If a token is not available this method will block until
  // the refill adds one to the bucket.
  bucket.consume(1);

  poll();
}
```