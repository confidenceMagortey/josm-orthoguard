/*
 * OrthoGuard - JOSM building geometry quality assurance plugin
 * Copyright (C) 2026 OrthoGuard contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 */
package org.openstreetmap.josm.plugins.orthoguard;

import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Destroyable;

/** JOSM entry point for OrthoGuard. */
public final class OrthoGuardPlugin extends Plugin implements Destroyable {
    private final OrthoGuardUploadHook uploadHook;

    public OrthoGuardPlugin(PluginInformation info) {
        super(info);
        MainApplication.getMenu().moreToolsMenu.add(new OrthoGuardAction());
        uploadHook = new OrthoGuardUploadHook();
        UploadAction.registerUploadHook(uploadHook);
    }

    /** Unregister the upload hook when JOSM unloads the plugin. */
    @Override
    public void destroy() {
        UploadAction.unregisterUploadHook(uploadHook);
    }
}
