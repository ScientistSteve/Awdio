package com.example.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.musicplayer.domain.*
import com.example.musicplayer.ui.theme.AwdioTheme
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels { val app = application as AwdioApplication; MainViewModelFactory(app.repository, app.playbackController) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { val s by vm.uiState.collectAsStateWithLifecycle(); AwdioTheme(s.themeMode) { AwdioApp(s, vm) } } }
}

@Composable fun AwdioApp(s: MainUiState, vm: MainViewModel) { val nav = rememberNavController(); Scaffold(topBar = { LargeTopAppBar(title = { Text("Awdio") }, actions = { IconButton({ nav.navigate("search") }) { Icon(Icons.Default.Search, "Search") }; ThemeMenu(s.themeMode, vm::theme) }) }, bottomBar = { Column { MiniPlayer(s, vm) { nav.navigate("now") }; NavigationBar { NavItem(nav,"home",Icons.Default.LibraryMusic,"Library"); NavItem(nav,"search",Icons.Default.Search,"Search"); NavItem(nav,"playlists",Icons.Default.QueueMusic,"Playlists") } } }) { p -> NavHost(nav, "home", Modifier.padding(p)) { composable("home") { HomeScreen(s, vm) }; composable("search") { SearchScreen(s, vm) }; composable("now") { NowPlayingScreen(s, vm) }; composable("playlists") { PlaylistsScreen(s, vm) } } } }
@Composable fun NavItem(nav: NavHostController, route: String, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) { val current = nav.currentBackStackEntryAsState().value?.destination?.route; NavigationBarItem(selected = current == route, onClick = { nav.navigate(route) { launchSingleTop = true } }, icon = { Icon(icon, label) }, label = { Text(label) }) }
@Composable fun ThemeMenu(mode: ThemeMode, set: (ThemeMode)->Unit) { var open by remember { mutableStateOf(false) }; IconButton({ open = true }) { Icon(Icons.Default.Contrast, "Theme") }; DropdownMenu(open, { open = false }) { ThemeMode.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { set(it); open=false }, leadingIcon = { if (mode==it) Icon(Icons.Default.Check, null) }) } } }
@Composable fun Artwork(song: Song?, size: Int) { Box(Modifier.size(size.dp).clip(RoundedCornerShape(18.dp)).background(Color(((song?.artworkSeed ?: 0x6750A4) or 0xFF000000).toInt())), contentAlignment = Alignment.Center) { Text(song?.title?.take(1) ?: "♪", style = MaterialTheme.typography.headlineLarge, color = Color.White) } }
@Composable fun SongRow(song: Song, liked: Boolean, onPlay:()->Unit, onFav:()->Unit) { ListItem(modifier = Modifier.clickable(onClick = onPlay), leadingContent = { Artwork(song, 56) }, headlineContent = { Text(song.title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${song.artist} • ${song.album}") }, trailingContent = { IconButton(onFav) { Icon(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite ${song.title}") } }) }
@Composable fun HomeScreen(s: MainUiState, vm: MainViewModel) { Column(Modifier.fillMaxSize()) { ScrollableTabRow(selectedTabIndex = s.libraryTab.ordinal) { LibraryTab.entries.forEach { Tab(selected=s.libraryTab==it,onClick={vm.tab(it)},text={Text(it.name)}) } }; when(s.libraryTab){ LibraryTab.Songs -> SongList(s.songs,s,vm); LibraryTab.Albums -> GroupList(s.songs.groupBy{it.album}) { vm.play(it.first()) }; LibraryTab.Artists -> GroupList(s.songs.groupBy{it.artist}) { vm.play(it.first()) }; LibraryTab.Playlists -> PlaylistsScreen(s,vm) } } }
@Composable fun SongList(list: List<Song>, s: MainUiState, vm: MainViewModel) { LazyColumn { items(list) { SongRow(it, it.id in s.favorites, { vm.play(it) }, { vm.favorite(it.id) }) } } }
@Composable fun GroupList(groups: Map<String,List<Song>>, play:(List<Song>)->Unit){ LazyColumn { items(groups.keys.toList()) { key -> ElevatedCard(Modifier.padding(12.dp).fillMaxWidth().clickable{ play(groups.getValue(key)) }) { ListItem(headlineContent={Text(key)}, supportingContent={Text("${groups.getValue(key).size} songs")}, leadingContent={Icon(Icons.Default.Album,null)}) } } } }
@Composable fun MiniPlayer(s: MainUiState, vm: MainViewModel, expand:()->Unit) { val song=s.playback.currentSong ?: return; ElevatedCard(Modifier.padding(8.dp).fillMaxWidth().clickable(onClick=expand)) { Row(Modifier.padding(8.dp), verticalAlignment=Alignment.CenterVertically) { Artwork(song,48); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){ Text(song.title); Text(song.artist, style=MaterialTheme.typography.bodySmall) }; IconButton({vm.playPause()}){Icon(if(s.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,"Play or pause") } } } }
@Composable fun NowPlayingScreen(s: MainUiState, vm: MainViewModel) { val song=s.playback.currentSong; Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally) { Artwork(song,280); Spacer(Modifier.height(24.dp)); Text(song?.title ?: "Nothing playing", style=MaterialTheme.typography.headlineMedium); Text(song?.artist ?: "Pick a song from Library"); Slider(value=s.playback.positionMs.toFloat(), onValueChange={vm.seek(it.toLong())}, valueRange=0f..max(1L,s.playback.durationMs).toFloat(), modifier=Modifier.semantics{contentDescription="Playback scrubber"}); Row{ IconButton({vm.shuffle()}){Icon(Icons.Default.Shuffle,"Shuffle")}; IconButton({vm.previous()}){Icon(Icons.Default.SkipPrevious,"Previous")}; FilledIconButton({vm.playPause()}, Modifier.size(64.dp)){Icon(if(s.playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,"Play pause")}; IconButton({vm.next()}){Icon(Icons.Default.SkipNext,"Next")}; IconButton({vm.repeat()}){Icon(Icons.Default.Repeat,"Repeat ${s.playback.repeatMode}")} }; AssistChip(onClick={vm.bass()}, label={Text(if(s.playback.bassBoost)"Bass boost on" else "Bass boost off")}); QueueCard(s,vm) } }
@Composable fun SearchScreen(s: MainUiState, vm: MainViewModel) { Column(Modifier.fillMaxSize().padding(16.dp)) { OutlinedTextField(s.searchQuery, vm::search, Modifier.fillMaxWidth(), label={Text("Search songs, artists, albums")}, leadingIcon={Icon(Icons.Default.Search,null)}); Spacer(Modifier.height(12.dp)); if(s.searchQuery.isBlank()) Text("Start typing to search your library.") else if(s.filteredSongs.isEmpty()) Text("No results for '${s.searchQuery}'.") else SongList(s.filteredSongs,s,vm) } }
@Composable fun PlaylistsScreen(s: MainUiState, vm: MainViewModel) { var name by remember { mutableStateOf("") }; LazyColumn(Modifier.fillMaxSize().padding(12.dp)) { item { Row { OutlinedTextField(name,{name=it},Modifier.weight(1f),label={Text("New playlist")}); Button({vm.createPlaylist(name);name=""}, Modifier.padding(start=8.dp)){Text("Create")} } }; items(s.playlists) { pl -> ElevatedCard(Modifier.padding(vertical=8.dp).fillMaxWidth()) { ListItem(headlineContent={Text(pl.name)}, supportingContent={Text("${pl.songIds.size} songs")}, trailingContent={IconButton({vm.deletePlaylist(pl.id)}){Icon(Icons.Default.Delete,"Delete playlist")}}); s.songs.forEach { song -> Row(Modifier.padding(horizontal=16.dp), verticalAlignment=Alignment.CenterVertically){ Checkbox(song.id in pl.songIds,{ if(it) vm.addToPlaylist(pl.id,song.id) else vm.removeFromPlaylist(pl.id,song.id) }); Text(song.title) } } } } } }
@Composable fun QueueCard(s: MainUiState, vm: MainViewModel) { ElevatedCard(Modifier.fillMaxWidth().padding(top=16.dp)) { Text("Up next", Modifier.padding(16.dp), style=MaterialTheme.typography.titleMedium); s.playback.queue.forEach { ListItem(headlineContent={Text(it.title)}, supportingContent={Text(it.artist)}, trailingContent={IconButton({vm.removeQueue(it.id)}){Icon(Icons.Default.Close,"Remove from queue")}}) }; Row(Modifier.padding(16.dp), verticalAlignment=Alignment.CenterVertically){ Text("Sleep timer"); Spacer(Modifier.width(8.dp)); FilterChip(s.playback.sleepTimerMinutes==15,{vm.sleep(15)},{Text("15m")}); Spacer(Modifier.width(8.dp)); FilterChip(s.playback.sleepTimerMinutes==null,{vm.sleep(null)},{Text("Off")}) } } }
