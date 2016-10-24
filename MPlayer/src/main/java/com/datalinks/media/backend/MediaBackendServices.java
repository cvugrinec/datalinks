/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datalinks.media.backend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author chris
 */
public class MediaBackendServices {
    
    final static Logger logger = Logger.getLogger("MediaBackendServices");

    
    public List<Media> getMediaItems(){
        RestTemplate restTemplate = new RestTemplate();
        Media[] mediaItems = restTemplate.getForObject("http://localhost:8080/listMedia2",Media[].class );
        List<Media> result = new ArrayList();
        
        //  TODO: NiceAFy the nodejs service, now giving me garbage...workaround is this
        result.addAll(Arrays.asList(mediaItems));
        return result;
    }
    
}
