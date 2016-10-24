/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.datalinks.media.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 *
 * @author chris
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaItems {

    private Media[] mediaitems;

    /**
     * @return the mediaItem
     */
    public Media[] getMediaItem() {
        return mediaitems;
    }

    /**
     * @param mediaItem the mediaItem to set
     */
    public void setMediaItem(Media[] mediaItem) {
        this.mediaitems   = mediaItem;
    }
}
