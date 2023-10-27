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
package provider.wz;

import android.content.res.AssetManager;
import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import provider.Data;
import provider.DataDirectoryEntry;
import provider.DataProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.io.InputStream;
import java.nio.file.Paths;

public class XMLWZFile implements DataProvider {
	private static final Logger log = LoggerFactory.getLogger(DataProvider.class);
	private final Path root;
    private final WZDirectoryEntry rootForNavigation;

    public XMLWZFile(Path fileIn) {
        root = fileIn;
        rootForNavigation = new WZDirectoryEntry(fileIn.getFileName().toString(), 0, 0, null);
        AssetManager assetManager = Server.getInstance().getContext().getAssets();
        fillMapleDataEntitys(root.toString(), rootForNavigation, assetManager);
    }

    private void fillMapleDataEntitys(String lroot, WZDirectoryEntry wzdir, AssetManager assetManager) {
        try {
            String[] stream = assetManager.list(lroot);
            for (String fileName : stream) {
                if (isDirectory(lroot + "/" + fileName, assetManager) && !fileName.endsWith(".img")) {
                    WZDirectoryEntry newDir = new WZDirectoryEntry(fileName, 0, 0, wzdir);
                    wzdir.addDirectory(newDir);
                    fillMapleDataEntitys(lroot + "/" + fileName, newDir, assetManager);
                } else if (fileName.endsWith(".xml")) {
                    wzdir.addFile(new WZFileEntry(fileName.substring(0, fileName.length() - 4), 0, 0, wzdir));
                }
            }
        } catch (IOException e) {
            log.warn("Can not open file/directory at " + lroot);
        }
    }

    public static boolean isDirectory(String fileName, AssetManager assetManager) {
        try {
            String[] assets = assetManager.list(fileName);

            if (assets.length == 0) {
                // It's a file
                return false;
            }
        } catch (IOException e) {
            log.warn("open file/directory failed");
        }
        return true;
    }

    @Override
    public synchronized Data getData(String path) {
        AssetManager assetManager = Server.getInstance().getContext().getAssets();
        String dataFile = root + "/" + path + ".xml";
        Path imageDataDir = Paths.get(root.toString(), path);
        if (!isAssetFileExist(assetManager, dataFile)) {
            return null;
        }
        final XMLDomMapleData domMapleData;
        try (InputStream fis = assetManager.open(dataFile)) {
            domMapleData = new XMLDomMapleData(fis, imageDataDir.getParent());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Datafile " + path + " does not exist in " + root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return domMapleData;
    }

    private boolean isAssetFileExist(AssetManager assetManager, String filePath) {
        try {
            InputStream inputStream = assetManager.open(filePath);
            inputStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

	@Override
	public DataDirectoryEntry getRoot() {
		return rootForNavigation;
	}
}