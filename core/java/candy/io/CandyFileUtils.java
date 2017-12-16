/*
 * Copyright (C) 2017 TeamNexus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package candy.io;

import java.io.File;
import java.io.IOException;

import android.os.FileUtils;

/**
 * @hide
 */
public class CandyFileUtils {

	public static String readLine(String path) {
		return readLine(path, 65536);
	}

	public static String readLine(String path, int max) {
		try {
			return FileUtils.readTextFile(new File(path), max, null).split("\\n")[0];
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		return null;
	}

    public static void writeLine(String path, String text) {
		if (!text.endsWith("\n"))
			text += "\n";
		
		try {
			FileUtils.stringToFile(path, text);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
    }

	public static boolean exists(String path) {
		return new File(path).exists();
	}

	public static boolean isFile(String path) {
		File f = new File(path);
		return f.exists() && f.isFile();
	}

	public static boolean isDirectory(String path) {
		File f = new File(path);
		return f.exists() && !f.isFile();
	}

	public static boolean isAccessible(String path) {
		File f = new File(path);
		return f.exists() && f.canRead() && f.canWrite();
	}

	public static boolean isReadable(String path) {
		File f = new File(path);
		return f.exists() && f.canRead();
	}

	public static boolean isWriteable(String path) {
		File f = new File(path);
		return f.exists() && f.canWrite();
	}

}
