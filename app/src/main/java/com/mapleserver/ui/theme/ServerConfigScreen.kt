package com.mapleserver.ui.theme

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.mapleserver.MainViewModel
import com.mapleserver.ServerConfig
import com.mapleserver.WorldProperties
import java.io.File


@Composable
fun ServerConfigScreen(context: Context, navController: NavHostController, mainViewModel: MainViewModel) {
    Scaffold(
        topBar = {
            IconButton(
                onClick = {
                    navController.popBackStack()
                }
            ) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer Toggle Button")
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        onSave(context, mainViewModel.serverConfig)
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                ) {
                    Text("Save Configuration")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ServerConfigProperties(mainViewModel.serverConfig)
            }
        }
    }
}

@Composable
fun ServerConfigProperties(modifiedServerConfig: ServerConfig) {
    var host by remember { mutableStateOf(modifiedServerConfig.server.HOST) }
    var lanHost by remember { mutableStateOf(modifiedServerConfig.server.LANHOST) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        TextField(
            value = host,
            onValueChange = { newHost ->
                host = newHost
                modifiedServerConfig.server.HOST = newHost
            },
            label = { Text("WAN_IP") },
            modifier = Modifier.padding(16.dp)
        )
        TextField(
            value = lanHost,
            onValueChange = { newLanHost ->
                lanHost = newLanHost
                modifiedServerConfig.server.LANHOST = lanHost
            },
            label = { Text("LAN_IP") },
            modifier = Modifier.padding(16.dp)
        )
        WorldsConfiguration(modifiedServerConfig.worlds)
    }
}

@Composable
fun WorldsConfiguration(worlds: List<WorldProperties>) {
    worlds.forEachIndexed { index, world ->
        var serverMsg by remember { mutableStateOf(world.server_message) }
        var eventMsg by remember { mutableStateOf(world.event_message) }
        var expRate by remember { mutableStateOf(world.exp_rate) }
        var mesoRate by remember { mutableStateOf(world.meso_rate) }
        var dropRate by remember { mutableStateOf(world.drop_rate) }
        var bossDropRate by remember { mutableStateOf(world.boss_drop_rate) }
        var questRate by remember { mutableStateOf(world.quest_rate) }
        var fishingRate by remember { mutableStateOf(world.fishing_rate) }
        var travelRate by remember { mutableStateOf(world.travel_rate) }
        var isExpanded by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .clickable {
                    isExpanded = !isExpanded
                }
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = LinearOutSlowInEasing
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("world ${index} properties")
                if (isExpanded) {
                    TextField(
                        value = serverMsg,
                        onValueChange = { newserverMsg ->
                            serverMsg = newserverMsg
                            world.server_message = serverMsg
                        },
                        label = { Text("server_message") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = eventMsg,
                        onValueChange = { neweventMsg ->
                            eventMsg = neweventMsg
                            world.event_message = eventMsg
                        },
                        label = { Text("event_message") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = expRate.toString(),
                        onValueChange = { newexpRate: String ->
                            expRate = newexpRate.toInt()
                            world.exp_rate = expRate
                        },
                        label = { Text("exp_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = mesoRate.toString(),
                        onValueChange = { newmesoRate: String ->
                            mesoRate = newmesoRate.toInt()
                            world.meso_rate = mesoRate
                        },
                        label = { Text("meso_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = dropRate.toString(),
                        onValueChange = { newdropRate: String ->
                            dropRate = newdropRate.toInt()
                            world.drop_rate = dropRate
                        },
                        label = { Text("drop_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = bossDropRate.toString(),
                        onValueChange = { newbossDropRate: String ->
                            bossDropRate = newbossDropRate.toInt()
                            world.boss_drop_rate = bossDropRate
                        },
                        label = { Text("boss_drop_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = questRate.toString(),
                        onValueChange = { newquestRate: String ->
                            questRate = newquestRate.toInt()
                            world.quest_rate = questRate
                        },
                        label = { Text("quest_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = fishingRate.toString(),
                        onValueChange = { newfishingRate: String ->
                            fishingRate = newfishingRate.toInt()
                            world.fishing_rate = fishingRate
                        },
                        label = { Text("fishing_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                    TextField(
                        value = travelRate.toString(),
                        onValueChange = { newtravelRate: String ->
                            travelRate = newtravelRate.toInt()
                            world.travel_rate = travelRate
                        },
                        label = { Text("travel_rate") },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

fun onSave(context: Context, modifiedServerConfig: ServerConfig) {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.propertyNamingStrategy = PropertyNamingStrategies.UPPER_SNAKE_CASE
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    mapper.writeValue(File(context.dataDir, "config.yaml"), modifiedServerConfig)
}