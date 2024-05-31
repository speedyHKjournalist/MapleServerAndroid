import android.content.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.navigation.NavHostController
import com.mapleserver.LogViewModel
import com.mapleserver.MainViewModel
import com.mapleserver.ServerParameter
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.StartButton
import com.mapleserver.ui.theme.StopButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun MainCompose(context: Context, navController: NavHostController, serverParams: ServerParameter, viewModel: MainViewModel) {
    val logView = LogViewModel(LocalContext.current)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val showProcessingBar by viewModel.showProcessingBar.collectAsState()
    val isStartButtonEnabled by viewModel.isStartButtonEnabled.collectAsState()
    val isStopButtonEnabled by viewModel.isStopButtonEnabled.collectAsState()

    MapleServerTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(200.dp)) {
                    Text("OpenMapleServer", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    Divider()
                    NavigationDrawerItem(
                        label = { Text(text = "ServerConfig") },
                        selected = false,
                        onClick = { navController.navigate("config_editor_screen") }
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "ImportDatabase") },
                        selected = false,
                        onClick = { navController.navigate("import_db_screen") }
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "ExportDatabase") },
                        selected = false,
                        onClick = { navController.navigate("export_db_screen") }
                    )
                }
            }
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DrawerToggleButton(drawerState, scope)
                        StartButton(
                            text = "Start",
                            isButtonEnabled = isStartButtonEnabled,
                            startMapleServer = {
                                startMapleServer(
                                    context,
                                    serverParams.serviceIntent
                                )
                            }
                        )
                        StopButton(
                            text = "Stop",
                            isButtonEnabled = isStopButtonEnabled,
                            stopMapleServer = {
                                stopMapleServer(
                                    context,
                                    serverParams.serviceIntent,
                                    serverParams.mConnection
                                )
                            }
                        )
                    }
                    DisplayCurrentIP(serverParams)
                    LogWindow(logView)

                }
                DisplayProcessingBar(showProcessingBar)
            }
        }
    }
}

@Composable
fun LogWindow(logView: LogViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(
                modifier = Modifier
                    .background(Color.White)
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = logView.logMessage.value,
                    modifier = Modifier
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState(0)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DisplayProcessingBar(showProcessingBar: Boolean) {
    if (showProcessingBar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
            )
        }
    }
}

@Composable
fun DisplayCurrentIP(serverinit: ServerParameter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "WAN_IP: ${serverinit.serverConfig.server.HOST}",
            modifier = Modifier.padding(9.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Text(
            text = "LAN_IP: ${serverinit.serverConfig.server.LANHOST}",
            modifier = Modifier.padding(9.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun DrawerToggleButton(drawerState: DrawerState, scope: CoroutineScope) {
    // Add a button to toggle the drawer
    IconButton(
        onClick = {
            scope.launch {
                drawerState.apply {
                    if (isClosed) open() else close()
                }
            }
        }) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer Toggle Button")
    }
}

fun startMapleServer(context: Context, serviceIntent : Intent) {
    startForegroundService(context, serviceIntent)
}

fun stopMapleServer(context: Context, serviceIntent : Intent, connection : ServiceConnection) {
    context.unbindService(connection)
    context.stopService(serviceIntent)
}