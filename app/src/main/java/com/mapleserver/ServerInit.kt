package com.mapleserver

import android.content.*
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File

class ServerInit(context: Context) {
    val serviceIntent = Intent(context, ServerService::class.java)
    val yamlMapper = YAMLMapper()
    val serverConfig: ServerConfig = yamlMapper.readValue(File(context.dataDir, "config.yaml"), ServerConfig::class.java)
}