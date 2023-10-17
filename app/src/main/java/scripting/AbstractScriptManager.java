/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import android.content.res.AssetManager;
import android.util.Log;
import client.Client;
import com.whl.quickjs.wrapper.QuickJSContext;
import com.whl.quickjs.wrapper.QuickJSException;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;

/**
 * @author Matze
 */
public abstract class AbstractScriptManager {
    private static final Logger log = LoggerFactory.getLogger(AbstractScriptManager.class);

    protected AbstractScriptManager() {
    }

    protected QuickJSContext getInvocableScriptEngine(String path) {
        AssetManager assetManager = Server.getInstance().getContext().getAssets();
        try(InputStream is = assetManager.open("scripts/" + path)) {
            try {
                QuickJSContext engine = QuickJSContext.create();
                StringBuilder stringBuilder = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    // Append the read data to the StringBuilder.
                    stringBuilder.append(new String(buffer, 0, bytesRead));
                }

                engine.evaluate(stringBuilder.toString());
                return engine;
            } catch (final QuickJSException t) {
                log.warn("Exception during script eval for file: {}", path, t);
                return null;
            }
        } catch (IOException ex) {
            Log.e("FILE ERROR", path + " does not exist");
            return null;
        }
    }

    protected QuickJSContext getInvocableScriptEngine(String path, Client c) {
        QuickJSContext engine = c.getScriptEngine("scripts/" + path);
        if (engine == null) {
            engine = getInvocableScriptEngine(path);
            c.setScriptEngine(path, engine);
        }

        return engine;
    }

    protected void resetContext(String path, Client c) {
        c.removeScriptEngine("scripts/" + path);
    }
}
