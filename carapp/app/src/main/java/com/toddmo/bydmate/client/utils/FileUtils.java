package com.toddmo.bydmate.client.helper;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    
    public static String getSDCardPath(Context context) {
        // 获取应用专属的外部存储 (推荐在 Android 10+ 中使用)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return context.getExternalFilesDir(null).getAbsolutePath();
        } else {
            // 或者直接使用传统的 SD 卡路径
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    public static String copyAssetFileToDir(Context ctx, String assetFilePath, String destinationDirPath) {
        return copyAssetFileToDir(ctx, assetFilePath, new File(destinationDirPath));
    }

    public static String copyAssetFileToDir(Context ctx, String assetFilePath, File destinationDir) {
//        String assetFilePath = "ffmpeg";
        String assetFileName = new File(assetFilePath).getName();
        File binaryFile = new File(destinationDir, assetFileName);

//        Log.d(TAG, "path = " + binaryFile.getPath());

        // 如果文件已存在且不需要更新，则跳过
//        if (binaryFile.exists()) {
//            return binaryFile.getAbsolutePath();
//        }

        try {
            // 从 assets 复制文件
            InputStream inputStream = ctx.getAssets().open(assetFilePath);
            FileOutputStream outputStream = new FileOutputStream(binaryFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            binaryFile.setReadable(true, false);

            return binaryFile.getAbsolutePath();
            // 设置可执行权限
//            binaryFile.setExecutable(true);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}