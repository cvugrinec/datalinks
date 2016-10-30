package com.datalinks.media.backend;

import com.datalinks.media.backend.storage.StorageFileNotFoundException;
import com.datalinks.media.backend.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FileUploadController {

    private final StorageService storageService;
    final static Logger logger = Logger.getLogger("FileUploadController");

    @RequestMapping(value="/goAmp",method = RequestMethod.GET)
    public ModelAndView goAmpPage(Model model,
                            @RequestParam("url") String amsurl){
        //return "redirect:amp.html?url="+amsurl;
        return new ModelAndView(new RedirectView("http://datalinks.nl/azure-demo.html?url="+amsurl, true));
    }
    
    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {

      
        model.addAttribute("files", storageService
                .loadAll()
                .map(path ->
                        MvcUriComponentsBuilder
                                .fromMethodName(FileUploadController.class, "serveFile", path.getFileName().toString())
                                .build().toString())
                .collect(Collectors.toList()));
        

        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename)  {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFilename()+"\"")
                .body(file);
    }

    @PostMapping("/")
    public String handleFileUpload( @RequestParam("file") MultipartFile file,
                                    @RequestParam("sleutel") String sleutel,
                                    RedirectAttributes redirectAttributes) {

        storageService.store(file);
        String fileName = file.getOriginalFilename();
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + fileName + "!");
        
        //String amsUrl = UriUtils.encode(AmsMediaHandler.uploadMediaToAMS(sleutel,fileName),"UTF-8");
        String amsUrl = "goAmp?url="+AmsMediaHandler.uploadMediaToAMS(sleutel,fileName);
        redirectAttributes.addFlashAttribute("uploadedmessage", amsUrl);
        logger.log(Level.INFO, "Setting href to AMS to:  {0} ",amsUrl);

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
