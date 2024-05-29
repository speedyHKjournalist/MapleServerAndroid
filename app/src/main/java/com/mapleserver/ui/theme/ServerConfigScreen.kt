package com.mapleserver.ui.theme

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.mapleserver.ServerInit
import com.mapleserver.ServerConfig
import com.mapleserver.WorldProperties
import java.io.File


@Composable
fun ServerConfigScreen(context: Context, navController: NavHostController, serverinit: ServerInit) {
    Scaffold(
        topBar = {
            IconButton(
                onClick = {
                    navController.navigate("main_screen")
                }
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back Button")
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
                        onSave(context, serverinit.serverConfig)
                        navController.navigate("main_screen")
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
                ServerConfigProperties(serverinit.serverConfig)
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
        ConfigurationTextField(
            value = host,
            label = "WAN_IP",
            onValueChange = { newHost ->
                host = newHost
                modifiedServerConfig.server.HOST = newHost
            },
            keyboardType = KeyboardType.Number
        )
        ConfigurationTextField(
            value = lanHost,
            label = "LAN_IP",
            onValueChange = { newLanHost ->
                lanHost = newLanHost
                modifiedServerConfig.server.LANHOST = newLanHost
            },
            keyboardType = KeyboardType.Number
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
                .clickable { isExpanded = !isExpanded }
                .animateContentSize(animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("World $index Properties", style = MaterialTheme.typography.bodyMedium)

                if (isExpanded) {
                    ConfigurationTextField(
                        value = serverMsg,
                        label = "Server Message",
                        onValueChange = { newServerMsg ->
                            serverMsg = newServerMsg
                            world.server_message = serverMsg
                        }
                    )
                    ConfigurationTextField(
                        value = eventMsg,
                        label = "Event Message",
                        onValueChange = { newEventMsg ->
                            eventMsg = newEventMsg
                            world.event_message = eventMsg
                        }
                    )
                    ConfigurationTextField(
                        value = expRate.toString(),
                        label = "EXP Rate",
                        onValueChange = { newExpRate ->
                            expRate = newExpRate.toIntOrNull() ?: expRate
                            world.exp_rate = expRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = mesoRate.toString(),
                        label = "Meso Rate",
                        onValueChange = { newMesoRate ->
                            mesoRate = newMesoRate.toIntOrNull() ?: mesoRate
                            world.meso_rate = mesoRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = dropRate.toString(),
                        label = "Drop Rate",
                        onValueChange = { newDropRate ->
                            dropRate = newDropRate.toIntOrNull() ?: dropRate
                            world.drop_rate = dropRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = bossDropRate.toString(),
                        label = "Boss Drop Rate",
                        onValueChange = { newBossDropRate ->
                            bossDropRate = newBossDropRate.toIntOrNull() ?: bossDropRate
                            world.boss_drop_rate = bossDropRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = questRate.toString(),
                        label = "Quest Rate",
                        onValueChange = { newQuestRate ->
                            questRate = newQuestRate.toIntOrNull() ?: questRate
                            world.quest_rate = questRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = fishingRate.toString(),
                        label = "Fishing Rate",
                        onValueChange = { newFishingRate ->
                            fishingRate = newFishingRate.toIntOrNull() ?: fishingRate
                            world.fishing_rate = fishingRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                    ConfigurationTextField(
                        value = travelRate.toString(),
                        label = "Travel Rate",
                        onValueChange = { newTravelRate ->
                            travelRate = newTravelRate.toIntOrNull() ?: travelRate
                            world.travel_rate = travelRate
                        },
                        keyboardType = KeyboardType.Number
                    )
                }
            }
        }
    }
}

@Composable
fun ConfigurationTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

fun onSave(context: Context, modifiedServerConfig: ServerConfig) {
    val mapper = ObjectMapper(YAMLFactory())
    mapper.propertyNamingStrategy = PropertyNamingStrategies.UPPER_SNAKE_CASE
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    mapper.writeValue(File(context.dataDir, "config.yaml"), modifiedServerConfig)
}