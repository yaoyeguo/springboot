package org.yaoyeguo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 *
 */

@SpringBootApplication
public class Application
{
    public static void main( String[] args )
    {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
    }
}
