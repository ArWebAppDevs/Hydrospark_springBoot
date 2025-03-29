package com.hydrospark.hydrospark.configuration;

import com.hydrospark.hydrospark.entities.Hydrospark;
import com.hydrospark.hydrospark.repositories.HydrosparkRepo;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;

@Aspect
@Component
public class GlobalAspect {

    @Autowired
    private HydrosparkRepo hydrosparkRepo;

    // This will match all methods with any number of parameters in the com.hydrospark.hydrospark.controllers package
    @Before("execution(* com.hydrospark.hydrospark.controllers..*(..))")
    public void beforeControllerExecution() throws SQLException {
        // Accessing the HttpSession from RequestContextHolder
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpSession session = attributes.getRequest().getSession();

            // Retrieving the image data and converting it to Base64
            Hydrospark hyd = hydrosparkRepo.findByName("Hydro1...").get(0);
            Blob blob = new SerialBlob(hyd.getImg());
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            String base64Image = Base64.getEncoder().encodeToString(bytes);

            // Storing the Base64 image string in the session
            session.setAttribute("img", base64Image);
            System.out.println("Before controller method is called...");
        }
    }
}
