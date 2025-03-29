package com.hydrospark.hydrospark.controllers;


import com.hydrospark.hydrospark.entities.*;
import com.hydrospark.hydrospark.repositories.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.Part;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static ch.qos.logback.core.util.StringUtil.capitalizeFirstLetter;

@Controller
@RequestMapping("admin")
public class Admin {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private HydrosparkRepo hydrosparkRepo;
    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private SubProdRepo subProdRepo;

    @Autowired
    private UserRepo userRepo;

    @GetMapping("")
    public String adminHome(HttpSession session,Model model) throws SQLException {
        Hydrospark hyd=hydrosparkRepo.findByName("Hydro1...").get(0);
        Blob blob = new SerialBlob(hyd.getImg());
        byte[] bytes = blob.getBytes(1, (int) blob.length());
        String base64Image = Base64.getEncoder().encodeToString(bytes);
        session.setAttribute("img", base64Image);
        System.out.println("Here");
        return "admin.html";
    }



    @GetMapping("/employeeLogin")
    public String getEmployeeLogin(HttpSession session){
        return "employeeSignin.html";
    }
    @PostMapping("/employeeLogin")
    public String PostEmployeeLogin(HttpServletRequest request, Model model, HttpSession session){
        System.out.println(session.getAttribute("error"));
        session.setAttribute("error","In employeeLogin post");
        String email=request.getParameter("Email");
        String password=request.getParameter("Password");
        List<Employee> employees=employeeRepo.findEmployeeByEmailAndPassword(email,password);
        System.out.println("Employees: "+employees);
        if(employees.size()>0){
            session.setAttribute("employee",employees.get(0).getEmail());
            session.removeAttribute("user");
            session.setAttribute("role",employees.get(0).getRole());
            System.out.println("Hereeeeeeeeeeeeeee");

        }
        else{
            session.setAttribute("error","email or password is wrong");
            return "employeeSignin.html";
        }
        return "redirect:/admin";
    }


    @GetMapping("/addEmployee")
    public String getAddEmployee(Model model,HttpSession session){
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")==null && validRoles.contains(session.getAttribute("role"))){
            return "redirect:/admin";
        }
        return "addEmployee.html";
    }
    public int findLastDigit(String email){
        List<String> validRoles=List.of("admin","manager");

        List<Character> numbers=List.of('1','2','3','4','5','6','7','8','9','0');
        String curr="";
        email=email.split("@")[0];
        System.out.println(email);
        for(int i=email.length()-1;i>=0;i--){
            if (numbers.contains(email.charAt(i))){
                curr+=email.charAt(i);
            }
        }
        System.out.println(curr);
        return Integer.parseInt(curr);
    }

    @PostMapping("/addEmployee")
    public String postAddEmployee(Model model,HttpServletRequest request,HttpSession session){
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")==null && validRoles.contains(session.getAttribute("role"))){
            return "redirect:/admin";
        }
        String firstName=request.getParameter("firstName");
        String lastName=request.getParameter("lastName");
        String role=request.getParameter("employeeRoles");
        List<Employee> employees=employeeRepo.findEmployeeByFullname(firstName,lastName);
        int mx=0;
        if (employees.size()>0){
            for(Employee emp:employees){
                mx=Math.max(mx,findLastDigit(emp.getEmail()));
            }
        }
        mx++;
        String email=firstName+lastName+mx+"@hydrospark.org";
        String password=capitalizeFirstLetter(firstName)+lastName+"@"+mx;
        Employee employee =new Employee(firstName,lastName,password,email,role);
        employeeRepo.save(employee);
        model.addAttribute("error","Employee added successfully with:- " +email+"  and password:- "+password);
        return "addEmployee.html";
    }


    @GetMapping("/addProduct")
    public String getAddProducts(HttpSession session){
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")!=null && validRoles.contains(session.getAttribute("role"))){
            return "addProducts.html";
        }
        return "redirect:/admin/error";

    }

    @PostMapping("/addProduct")
    public String postAddProducts(HttpServletRequest request,HttpSession session, Model model) throws ServletException, IOException {
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")!=null && validRoles.contains(session.getAttribute("role"))) {

            String prodName=request.getParameter("ProductName");
            String subtype=request.getParameter("Category");
            String description=request.getParameter("Description");
            double price= Double.parseDouble(request.getParameter("price"));
            System.out.println("asdfghjk");
            Part filePart = request.getPart("ProductImage");

            long fileSizeInBytes = filePart.getSize();
            long fileSizeInKB = fileSizeInBytes / 1024;
            if (fileSizeInKB > 50) {
                model.addAttribute("error","Pic size should be less than 50 KB");
                System.out.println("size exceed");
                return "addProducts.html";

            }
            byte[] imageBytes = filePart.getInputStream().readAllBytes();
            Product getProd=productRepo.findByName(prodName);
            if (getProd==null){
                Product product=new Product(prodName,imageBytes);
                productRepo.save(product);
//               System.out.println(this.getFileName(filePart));
            }
            getProd=productRepo.findByName(prodName);
            SubProducts subProd=new SubProducts(subtype, price, description, getProd,imageBytes);
            subProdRepo.save(subProd);

            return "redirect:/admin/products";
        }
        else{
            return "redirect:/admin/error";
        }

    }

    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String cd : contentDisposition.split(";")) {
            if (cd.trim().startsWith("filename")) {
                // Extract the file name (remove leading and trailing spaces and quotes)
                String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return fileName;
            }
        }
        return null; // Return null if no filename found
    }

    @GetMapping("/prod/{id}")
    public String getProduct(@PathVariable int id,Model model) throws SQLException {
        Product product = productRepo.findById(id);
        Blob blob = new SerialBlob(product.getProdImg());
        byte[] bytes = blob.getBytes(1, (int) blob.length());
        String base64Image = Base64.getEncoder().encodeToString(bytes);
        model.addAttribute("base64Image", base64Image);
        if (product == null) {
            model.addAttribute("message", "Product not found");
            return "error";
        }

        if (product.getProdImg() != null) {
            model.addAttribute("image", product.getProdImg());
        }

        model.addAttribute("product", product);
        return "img.html";
    }

    @GetMapping("/image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getProductImage(@PathVariable int id) {
        Product product = productRepo.findById(id);

        if (product == null || product.getProdImg() == null) {
            return ResponseEntity.notFound().build(); // Return 404 if product or image is not found
        }

        // Set the appropriate media type (here assuming it's a JPEG, but you can change it based on the image format)
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // Change this depending on your image format (e.g., IMAGE_PNG for PNG files)
                .body(product.getProdImg()); // Return the image as a byte array
    }

    @GetMapping("/removeEmployee")
    public String getRemoveEmployee(HttpSession session){
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")!=null && validRoles.contains(session.getAttribute("role"))){
            return "removeEmployee.html";
        }
        return "redirect:/admin/error";
    }

    @PostMapping("/removeEmployee")
    public String postRemoveEmployee(HttpServletRequest request,HttpSession session,Model model){
        List<String> validRoles=List.of("admin","manager");
        if(session.getAttribute("employee")!=null && validRoles.contains(session.getAttribute("role"))){
            String empEmail=request.getParameter("Email");
            Employee employee=employeeRepo.findEmployeeByEmail(empEmail).get(0);
            employeeRepo.delete(employee);
            model.addAttribute("error","Employee with email "+empEmail+" removed successfully");
            return "removeEmployee.html";
        }
        return "redirect:/admin/error";
    }

    @GetMapping("/showvisited")
    public String shoVistedUsers(Model model,HttpSession session){
        List<String> roles=List.of("admin","manager");
        if(session.getAttribute("employee")!=null && roles.contains(session.getAttribute("role").toString().toLowerCase())){
            List<User> allUser=(userRepo.findAll());
            List<Map<String,String>> visited=new ArrayList<>();
            int c=0;
            for(User user:allUser){
                if (user.visitedProduct==true && user.contacted==false){
                    c++;
                    Map<String,String> map=new HashMap<>();
                    map.put("id", String.valueOf(user.Id));
                    map.put("firstName",user.firstName);
                    map.put("lastName",user.lastName);
                    map.put("email",user.email);
                    map.put("number",user.number+"");
                    map.put("contacted",user.contacted+"");
                    map.put("url","/admin/contacted/"+user.Id);
                    map.put("date", String.valueOf(user.dateOfProductVisit).split(" ")[0]);
                    visited.add(map);
                }
            }
            visited.sort((u1, u2) -> u1.get("date").compareTo(u2.get("date")));
            if (c>0){
                model.addAttribute("visited",visited);
            }
            else{
                model.addAttribute("visited",null);
            }


            return "visited";
        }
        return "redirect:/admin";
    }

    @PostMapping("/contacted/{email}")
    public String contacted(@PathVariable String email){
        email = URLDecoder.decode(email, StandardCharsets.UTF_8);
        User user=userRepo.findByEmail(email).get(0);
        System.out.println(user.contacted);
        user.contacted=false;
        user.visitedProduct=false;
        userRepo.save(user);
        System.out.println(user.contacted);
        return "redirect:/admin/showvisited";
    }
    @GetMapping("/profile")
    public String Userprofile(HttpSession session,Model model){
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
        if(employee!=null){
            Employee userProfile=employeeRepo.findEmployeeByEmail(employee).get(0);
            System.out.printf(userProfile.getEmail());
            model.addAttribute("firstName",userProfile.getFirstName());
            model.addAttribute("lastName",userProfile.getLastName());
            model.addAttribute("email",userProfile.getEmail());
            model.addAttribute("password",userProfile.getPassword());
            model.addAttribute("role",userProfile.getRole());

        }
        return "profile.html";
    }

//    @GetMapping("/products")
//    public String allproducts(Model model,HttpSession session){
//        String employee= (String) session.getAttribute("employee");
//        if(session.getAttribute("employee")==null){
//            return "redirect:/admin";
//        }
//        List<Product> allProducts=productRepo.findAll();
//        System.out.println("Prds"+allProducts);
//        List<Map<String,String>> prods=new ArrayList<>();
//        for(Product product:allProducts){
//            Map<String,String> map=new HashMap<>();
//            String prodname=product.getProductName();
//            map.put("prodId",product.getProId()+"");
//            map.put("prodName",prodname);
//            map.put("removeproduct","/admin/removeproduct/"+product.getProId());
//            map.put("editProduct","/admin/editproduct/"+product.getProId());
//            prods.add(map);
//        }
//        List<SubProducts> allSubProducts=subProdRepo.findAll();
//        System.out.println("subProds"+allSubProducts);
//        List<Map<String,String>> subProds=new ArrayList<>();
//        for(SubProducts subProd:allSubProducts){
//            Map<String,String> map=new HashMap<>();
//            String subprodname=subProd.getSubTypeName();
//            map.put("subProdId",subProd.getSubProdId()+"");
//            map.put("subprodName",subProd.getSubTypeName());
//            map.put("removesubproduct","/admin/removesubproduct/"+subProd.getSubProdId());
//            map.put("editsubproduct","/admin/editsubproduct/"+subProd.getSubProdId());
//            prods.add(map);
//        }
//
//        System.out.println(prods.size());
////        for(Map<String,String> m:prods){
////            for(String s:m.keySet()){
////                System.out.println(m.get(s));
////            }
////        }
//        model.addAttribute("products",prods);
//        return "allproducts";
//    }
//


//@GetMapping("/products")
//public String allproducts(Model model, HttpSession session) {
//    String employee = (String) session.getAttribute("employee");
//    if (session.getAttribute("employee") == null) {
//        return "redirect:/admin";
//    }
//
//    // Fetch all products
//    List<Product> allProducts = productRepo.findAll();
//    System.out.println("Prds" + allProducts);
//    List<Map<String, String>> prods = new ArrayList<>();
//    for (Product product : allProducts) {
//        Map<String, String> map = new HashMap<>();
//        String prodname = product.getProductName();
//        map.put("prodId", String.valueOf(product.getProId()));
//        map.put("prodName", prodname);
//        map.put("removeproduct", "/admin/removeproduct/" + product.getProId());
//        map.put("editProduct", "/admin/editproduct/" + product.getProId());
//        prods.add(map);
//    }
//
//    // Fetch all sub-products
//    List<SubProducts> allSubProducts = subProdRepo.findAll();
//    System.out.println("subProds" + allSubProducts);
//    List<Map<String, String>> subProds = new ArrayList<>();
//    for (SubProducts subProd : allSubProducts) {
//        Map<String, String> map = new HashMap<>();
//        String subprodname = subProd.getSubTypeName();
//        map.put("subProdId", String.valueOf(subProd.getSubProdId()));
//        map.put("subprodName", subprodname);
//        map.put("removesubproduct", "/admin/removesubproduct/" + subProd.getSubProdId());
//        map.put("editsubproduct", "/admin/editsubproduct/" + subProd.getSubProdId());
//        subProds.add(map);
//    }
//
//    // Pass both lists to the template
//    model.addAttribute("products", prods);
//    model.addAttribute("subProducts", subProds);
//
//    return "allproducts";
//}
//
//@GetMapping("/products")
//public String allproducts(@RequestParam(value = "search", required = false) String search, Model model, HttpSession session) {
//    String employee = (String) session.getAttribute("employee");
//    if (employee == null) {
//        return "redirect:/admin";
//    }
//
//    // Fetch all products
//    List<Product> allProducts = productRepo.findAll();
//    if (search != null && !search.isEmpty()) {
//        allProducts = allProducts.stream()
//                .filter(p -> p.getProductName().toLowerCase().contains(search.toLowerCase()))
//                .collect(Collectors.toList());
//    }
//
//    List<Map<String, String>> prods = new ArrayList<>();
//    for (Product product : allProducts) {
//        Map<String, String> map = new HashMap<>();
//        String prodname = product.getProductName();
//        map.put("prodId", String.valueOf(product.getProId()));
//        map.put("prodName", prodname);
//        map.put("removeproduct", "/admin/removeproduct/" + product.getProId());
//        map.put("editProduct", "/admin/editproduct/" + product.getProId());
//        prods.add(map);
//    }
//
//    // Fetch all sub-products
//    List<SubProducts> allSubProducts = subProdRepo.findAll();
//    if (search != null && !search.isEmpty()) {
//        allSubProducts = allSubProducts.stream()
//                .filter(sp -> sp.getSubTypeName().toLowerCase().contains(search.toLowerCase()))
//                .collect(Collectors.toList());
//    }
//
//    List<Map<String, String>> subProds = new ArrayList<>();
//    for (SubProducts subProd : allSubProducts) {
//        Map<String, String> map = new HashMap<>();
//        String subprodname = subProd.getSubTypeName();
//        map.put("subProdId", String.valueOf(subProd.getSubProdId()));
//        map.put("subprodName", subprodname);
//        map.put("removesubproduct", "/admin/removesubproduct/" + subProd.getSubProdId());
//        map.put("editsubproduct", "/admin/editsubproduct/" + subProd.getSubProdId());
//        subProds.add(map);
//    }
//
//    // Pass both lists and the search parameter to the template
//    model.addAttribute("products", prods);
//    model.addAttribute("subProducts", subProds);
//    model.addAttribute("search", search);
//
//    return "allproducts";
//}
@GetMapping("/products")
public String allProducts(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "subsearch", required = false) String subsearch,
        Model model,
        HttpSession session) {
    String employee = (String) session.getAttribute("employee");
    if (employee == null) {
        return "redirect:/admin";
    }

    // Fetch all products
    List<Product> allProducts = productRepo.findAll();
    List<Map<String, String>> prods = new ArrayList<>();

    // Filter products if search is provided
    if (search != null && !search.isEmpty()) {
        allProducts = allProducts.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    for (Product product : allProducts) {
        Map<String, String> map = new HashMap<>();
        String prodname = product.getProductName();
        map.put("prodId", String.valueOf(product.getProId()));
        map.put("prodName", prodname);
        map.put("removeproduct", "/admin/removeproduct/" + product.getProId());
        map.put("editProduct", "/admin/editproduct/" + product.getProId());
        prods.add(map);
    }

    // Fetch all sub-products
    List<SubProducts> allSubProducts = subProdRepo.findAll();
    List<Map<String, String>> subProds = new ArrayList<>();

    // Filter sub-products if subsearch is provided
    if (subsearch != null && !subsearch.isEmpty()) {
        allSubProducts = allSubProducts.stream()
                .filter(sp -> sp.getSubTypeName().toLowerCase().contains(subsearch.toLowerCase()))
                .collect(Collectors.toList());
    }

    for (SubProducts subProd : allSubProducts) {
        Map<String, String> map = new HashMap<>();
        String subprodname = subProd.getSubTypeName();
        map.put("subProdId", String.valueOf(subProd.getSubProdId()));
        map.put("subprodName", subprodname);
        map.put("removesubproduct", "/admin/removesubproduct/" + subProd.getSubProdId());
        map.put("editsubproduct", "/admin/editsubproduct/" + subProd.getSubProdId());
        subProds.add(map);
    }

    // Add attributes to model
    model.addAttribute("products", prods);
    model.addAttribute("subProducts", subProds);
    model.addAttribute("search", search);
    model.addAttribute("subsearch", subsearch);

    return "allproducts";
}
    @GetMapping("/products/{search}")
    public String searchProducts(@PathVariable("search") String search, Model model, HttpSession session) {
        String employee = (String) session.getAttribute("employee");
        if (session.getAttribute("employee") == null) {
            return "redirect:/admin";
        }

        List<Product> allProducts = productRepo.findAll();
        List<Map<String, String>> prods = new ArrayList<>();
        for (Product product : allProducts) {
            Map<String, String> map = new HashMap<>();
            String prodname = product.getProductName();

            // Apply search filter for products
            if (search != null && !search.equals("search") && !prodname.toLowerCase().contains(search.toLowerCase())) {
                continue;
            }

            map.put("prodId", String.valueOf(product.getProId()));
            map.put("prodName", prodname);
            map.put("removeproduct", "/admin/removeproduct/" + product.getProId());
            map.put("editProduct", "/admin/editproduct/" + product.getProId());
            prods.add(map);
        }

        model.addAttribute("products", prods);
        model.addAttribute("search", search);
        return "allproducts"; // your view name
    }

    @GetMapping("/subproducts/{subsearch}")
    public String searchSubProducts(@PathVariable("subsearch") String subsearch, Model model, HttpSession session) {
        String employee = (String) session.getAttribute("employee");
        if (session.getAttribute("employee") == null) {
            return "redirect:/admin";
        }

        List<SubProducts> allSubProducts = subProdRepo.findAll();
        List<Map<String, String>> subProds = new ArrayList<>();
        for (SubProducts subProd : allSubProducts) {
            Map<String, String> map = new HashMap<>();
            String subprodname = subProd.getSubTypeName();

            // Apply subsearch filter for sub-products
            if (subsearch != null && !subsearch.equals("subsearch") && !subprodname.toLowerCase().contains(subsearch.toLowerCase())) {
                continue;
            }

            map.put("subProdId", String.valueOf(subProd.getSubProdId()));
            map.put("subprodName", subprodname);
            map.put("removesubproduct", "/admin/removesubproduct/" + subProd.getSubProdId());
            map.put("editsubproduct", "/admin/editsubproduct/" + subProd.getSubProdId());
            subProds.add(map);
        }

        model.addAttribute("subProducts", subProds);
        model.addAttribute("subsearch", subsearch);
        return "allproducts"; // your view name
    }



    @PostMapping("/removeproduct/{prodId}")
    @Transactional
    public String removeProduct(@PathVariable int prodId,HttpSession session){
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
//        productName = URLDecoder.decode(productName, StandardCharsets.UTF_8);
        Product prod=productRepo.findById(prodId);
        productRepo.deleteById(prod.getProId());
        return "redirect:/admin/products";
    }
    @PostMapping("/removesubproduct/{subprodId}")
    @Transactional
    public String removeSubProduct(@PathVariable int subprodId,HttpSession session){
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
//        subProduct = URLDecoder.decode(subProduct, StandardCharsets.UTF_8);
        SubProducts subProd=subProdRepo.findSubProductById(subprodId).get(0);
        subProdRepo.deleteById(subProd.getSubProdId());
        return "redirect:/admin/products";
    }

    @GetMapping("/editproduct/{prodId}")
    public String getEditProduct(@PathVariable int prodId,Model model,HttpSession session){
        System.out.println(prodId);
        String employee= (String) session.getAttribute("employee");
            if(session.getAttribute("employee")==null){
                return "redirect:/admin";
            }
            model.addAttribute("redirect","/admin/editproduct/"+prodId);
            return "editProduct.html";
    }
    @PostMapping("/editproduct/{prodId}")
    public String editProduct(@PathVariable int prodId,HttpServletRequest request,HttpSession session,Model model) throws ServletException, IOException {
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
        System.out.println("Editing product");
//        productName = URLDecoder.decode(productName, StandardCharsets.UTF_8);
        Product prod=productRepo.findById(prodId);

        String prodName=request.getParameter("ProductName");

        Part filePart = request.getPart("ProductImage");
        long fileSizeInBytes = filePart.getSize();
        long fileSizeInKB = fileSizeInBytes / 1024;
        if (fileSizeInKB > 50) {
            model.addAttribute("error","Pic size should be less than 50 KB");
            System.out.println("size exceed");
            return "addProducts.html";

        }
        byte[] imageBytes = filePart.getInputStream().readAllBytes();
        System.out.println("Size "+fileSizeInKB);
        if(prodName!=null){
            prod.setProductName(prodName);
        }

        if(fileSizeInKB>0){
            prod.setProdImg(imageBytes);
        }

        productRepo.save(prod);
        model.addAttribute("error","Product edited sucessfully");
//        productRepo.deleteById(prod.getProId());

        return "redirect:/admin/products";
    }

    @GetMapping("/editsubproduct/{subprodId}")
    public String getEditSubProduct(@PathVariable int subprodId,HttpSession session,Model model) throws ServletException, IOException {
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
        model.addAttribute("redirect","/admin/editsubproduct/"+subprodId);
//        productRepo.deleteById(prod.getProId());
        return "editProduct.html";
    }

    @PostMapping("/editsubproduct/{subprodId}")
    public String postEditSubProduct(@PathVariable int subprodId,HttpSession session,HttpServletRequest request,Model model) throws ServletException, IOException {
        String employee= (String) session.getAttribute("employee");
        if(session.getAttribute("employee")==null){
            return "redirect:/admin";
        }
        SubProducts subProduct=subProdRepo.findSubProductById(subprodId).get(0);
        String prodName=request.getParameter("ProductName");
        String description=request.getParameter("Description");
        String price= request.getParameter("price");
        Part filePart = request.getPart("ProductImage");

        System.out.println(prodName);
        System.out.println(description);
        System.out.println(price);
        System.out.println(subprodId);
        System.out.println(filePart);

//        if(subProducts==null){
//            model.addAttribute("error","No product Found")
//            return "allproducts";
//        }
        long fileSizeInBytes = filePart.getSize();
        long fileSizeInKB = fileSizeInBytes / 1024;
        if (fileSizeInKB > 50) {
            model.addAttribute("error","Pic size should be less than 50 KB");
            System.out.println("size exceed");
            return "addProducts.html";

        }

        if(prodName.length()>0){
            subProduct.setSubTypeName(prodName);
        }
        if(description.length()>0){
            subProduct.setDescription(description);
        }
        if(price.length()>0){
            subProduct.setSubTypePrice(Double.parseDouble(price));
        }


        if(fileSizeInKB>0){
            byte[] imageBytes = filePart.getInputStream().readAllBytes();
            subProduct.setSubProdImg(imageBytes);
        }
        System.out.println("Edit Herererererererere");
        subProdRepo.save(subProduct);
//        productRepo.deleteById(prod.getProId());
        return "redirect:/admin/products";
    }


    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return  "redirect:/admin";
    }
    @GetMapping("/error")
    public String error(){
        return "unauthorized.html";
    }

}
