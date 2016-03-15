package servlets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * This servlet used to upload RDF files that belongs to dataset(s) (version)
 *
 * JCH-NOTE:Not currently used
 */
public class UploadFile extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private String UPLOAD_DIRECTORY = "";

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        
        ServletContext servletContext = getServletContext();
        
        String genericConfigFilePath = OntologyQueryServlet.getConfigFilePath(servletContext, null);
        UPLOAD_DIRECTORY = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_Files_Folder");
        String filePathname = "";
        String contextPath = servletContext.getRealPath("/");
        // process only if its multipart content
        if (isMultipart) {
            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                // Parse the request
                List<FileItem> multiparts = upload.parseRequest(request);

                for (FileItem item : multiparts) {
                    if (!item.isFormField()) {
                        String name = new File(item.getName()).getName();
                        //Since I use contextPath in file local paths are supported
                        filePathname = contextPath + UPLOAD_DIRECTORY + File.separator + name;
                        item.write(new File(filePathname));
                    }
                }
            } catch (Exception e) {
                System.out.println("File upload failed!");
            }
        }
    }
}
