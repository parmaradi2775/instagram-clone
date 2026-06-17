package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CommentEntity
import com.example.data.PostEntity
import com.example.data.StoryEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramMainView(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activePostComments by viewModel.activePostForComments.collectAsStateWithLifecycle()
    val activeStoryIndex by viewModel.activeStoryIndex.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDirectInboxDialog by remember { mutableStateOf(false) }
    var showSavedToast by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentTab != InstaTab.REELS) {
                    InstagramTopAppBar(
                        onInboxClick = { showDirectInboxDialog = true },
                        onNotificationClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Notifications: 3 new likes on your travel post!")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                InstagramBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentTab, label = "tabScreenFade") { tab ->
                    when (tab) {
                        InstaTab.HOME -> HomeFeedScreen(
                            viewModel = viewModel,
                            onStoryClick = { index -> viewModel.startStoryViewing(index) }
                        )
                        InstaTab.SEARCH -> SearchExploreScreen(viewModel = viewModel)
                        InstaTab.CREATE -> CreatePostScreen(viewModel = viewModel)
                        InstaTab.REELS -> ReelsScreen(viewModel = viewModel)
                        InstaTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Expanded Comments Bottom Sheet overlay
        activePostComments?.let { post ->
            CommentsBottomSheetOverlay(
                post = post,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissComments() }
            )
        }

        // Fullscreen Story Viewer Overlay
        activeStoryIndex?.let { index ->
            if (index in stories.indices) {
                StoryViewerOverlay(
                    story = stories[index],
                    onNext = { viewModel.nextStory() },
                    onPrev = { viewModel.previousStory() },
                    onClose = { viewModel.closeStories() }
                )
            }
        }

        // Simulated Direct Inbox Dialog
        if (showDirectInboxDialog) {
            SimulatedInboxDialog(onDismiss = { showDirectInboxDialog = false })
        }
    }
}

// TOP BAR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramTopAppBar(
    onInboxClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Text(
                text = "Instagram",
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .offset(y = (-2).dp)
                    .drawBehind {
                        // Subtle stylish brand underline accent
                    }
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier.testTag("notification_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Notifications",
                    tint = Color(0xFFB3261E), // Red Accent from High Density specs
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onInboxClick,
                modifier = Modifier.testTag("inbox_button")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Direct Messages",
                        modifier = Modifier.size(24.dp)
                    )
                    // Notification small red badge from HTML setup:
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFB3261E), CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = 1.dp, y = (-1).dp)
                    )
                }
            }
        }
    )
}

// BOTTOM NAVIGATION BAR
@Composable
fun InstagramBottomNavigation(
    currentTab: InstaTab,
    onTabSelected: (InstaTab) -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        color = Color(0xFFF3EDF7), // Light Lavender surface from High Density specs
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                InstaTab.HOME to Icons.Default.Home,
                InstaTab.SEARCH to Icons.Default.Search,
                InstaTab.CREATE to Icons.Default.AddCircle,
                InstaTab.REELS to Icons.Default.PlayArrow,
                InstaTab.PROFILE to Icons.Default.Person
            ).forEach { (tab, icon) ->
                val selected = currentTab == tab
                val scale by animateFloatAsState(targetValue = if (selected) 1.15f else 1.0f, label = "tabAnimScale")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFFE8DEF8) else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.name,
                            tint = if (selected) Color(0xFF1D192B) else Color(0xFF49454F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// FEED SCREEN
@Composable
fun HomeFeedScreen(
    viewModel: InstagramViewModel,
    onStoryClick: (Int) -> Unit
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Stories bar
        item {
            StoriesSection(stories = stories, onStoryClick = onStoryClick)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        // Empty state block
        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = InstaPink)
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    onLikeToggle = { viewModel.toggleLike(post.id) },
                    onCommentsClick = { viewModel.showCommentsForPost(post) }
                )
            }
        }
    }
}

// STORIES SECTION
@Composable
fun StoriesSection(
    stories: List<StoryEntity>,
    onStoryClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(stories.size) { index ->
            val story = stories[index]
            val hasUnviewed = !story.isViewed

            val ringBrush = if (hasUnviewed) {
                Brush.sweepGradient(listOf(InstaPurple, InstaPink, InstaOrange, InstaYellow, InstaPurple))
            } else {
                Brush.sweepGradient(listOf(Color.LightGray, Color.Gray, Color.LightGray))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStoryClick(index) }
                    .testTag("story_bubble_$index")
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            width = if (hasUnviewed) 2.5.dp else 1.25.dp,
                            brush = ringBrush,
                            shape = CircleShape
                        )
                        .padding(if (hasUnviewed) 3.dp else 1.5.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                ) {
                    Image(
                        painter = painterResource(id = story.authorAvatarResId),
                        contentDescription = story.authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = story.authorName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF49454F)
                )
            }
        }
    }
}

// SINGLE POST CARD ITEM
@Composable
fun PostItem(
    post: PostEntity,
    onLikeToggle: () -> Unit,
    onCommentsClick: () -> Unit
) {
    var isLikedAnim by remember { mutableStateOf(false) }
    val darkTheme = isSystemInDarkTheme()
    val cardBg = if (darkTheme) Color(0xFF1C1B1F) else Color.White

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp), // High Density rounded card floating look
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("post_card_${post.id}")
    ) {
        Column {
            // Post Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFEADDFF), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = post.authorAvatarResId),
                        contentDescription = post.authorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (post.location.isNotBlank()) {
                        Text(
                            text = post.location,
                            fontSize = 10.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Post Visual Media Container with Double-Tap to Like Detector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                isLikedAnim = true
                                if (!post.isLiked) {
                                    onLikeToggle()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // If customized gradient, render custom background, else standard image
                if (post.imageResId == 0 && post.gradientStartHex != null && post.gradientEndHex != null) {
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color(android.graphics.Color.parseColor(post.gradientStartHex)),
                            Color(android.graphics.Color.parseColor(post.gradientEndHex))
                        )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.caption.substringBefore("[Filter:"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    val filterApplied = post.caption.substringAfter("[Filter: ", "").substringBefore("]", "")
                    val colorFilter = remember(filterApplied) { getFilterMatrix(filterApplied) }

                    Image(
                        painter = painterResource(id = post.imageResId),
                        contentDescription = "Post Media",
                        contentScale = ContentScale.Crop,
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Heart double tap animation overlay
                if (isLikedAnim) {
                    LaunchedEffect(key1 = isLikedAnim) {
                        delay(750)
                        isLikedAnim = false
                    }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked Splash",
                            tint = Color(0xFFB3261E), // Red active like color
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Action row buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onLikeToggle,
                    modifier = Modifier.testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color(0xFFB3261E) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onCommentsClick,
                    modifier = Modifier.testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bookmark/Save toggle simulation
                var isSaved by remember { mutableStateOf(false) }
                IconButton(onClick = { isSaved = !isSaved }) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Save Post",
                        tint = if (isSaved) GoldYellow else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Likes Text indicator
            Text(
                text = "${post.likesCount} likes",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Caption Text
            val cleanCaption = post.caption.substringBefore("[Filter:")
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("${post.authorName} ")
                        }
                        // Highlight hashtags with core design Accent Purple #6750A4
                        val words = cleanCaption.split(" ")
                        words.forEachIndexed { i, word ->
                            if (word.startsWith("#")) {
                                withStyle(style = SpanStyle(color = Color(0xFF6750A4), fontWeight = FontWeight.SemiBold)) {
                                    append(word)
                                }
                            } else {
                                append(word)
                            }
                            if (i < words.lastIndex) {
                                append(" ")
                            }
                        }
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Comments preview hyperlink text
            Text(
                text = "View comments...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clickable { onCommentsClick() }
            )

            // Timestamp indicator
            Text(
                text = "Just now",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }
    }
}

// COMMENTS SLIDE SHEETS OVERLAY
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheetOverlay(
    post: PostEntity,
    viewModel: InstagramViewModel,
    onDismiss: () -> Unit
) {
    val comments by viewModel.activeComments.collectAsStateWithLifecycle()
    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            // Header bar
            Text(
                text = "Comments",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            HorizontalDivider()

            // List of comments
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Post author caption displayed as parent comment
                item {
                    val cleanCaption = post.caption.substringBefore("[Filter:")
                    Row(verticalAlignment = Alignment.Top) {
                        Image(
                            painter = painterResource(id = post.authorAvatarResId),
                            contentDescription = "Post Author Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("${post.authorName} ")
                                    }
                                    append(cleanCaption)
                                },
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Author",
                                fontSize = 11.sp,
                                color = InstaPink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }

                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No comments yet. Start the conversation!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(comments) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                // Default profile avatar representation helper
                                val avatarResId = if (comment.authorName == "you") {
                                    R.drawable.img_avatar_admin
                                } else {
                                    // Seed other ones based on usernames
                                    when (comment.authorName) {
                                        "travel_explorer" -> R.drawable.img_post_travel
                                        "cafe_lover" -> R.drawable.img_post_cafe
                                        "neon_designer" -> R.drawable.img_post_creative
                                        else -> R.drawable.img_avatar_admin
                                    }
                                }
                                Image(
                                    painter = painterResource(id = avatarResId),
                                    contentDescription = comment.authorName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("${comment.authorName} ")
                                        }
                                        append(comment.content)
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Just now • Reply",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Custom Comment Bar TextField
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_avatar_admin),
                    contentDescription = "My Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add comment as you...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commentText.isNotBlank()) {
                            viewModel.addCommentToPost(post.id, commentText)
                            commentText = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    })
                )

                TextButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addCommentToPost(post.id, commentText)
                            commentText = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    modifier = Modifier.testTag("comment_submit_button")
                ) {
                    Text("Post", fontWeight = FontWeight.Bold, color = if (commentText.isNotBlank()) InstaBlue else Color.Gray)
                }
            }
        }
    }
}

// SEARCH/EXPLORE SCREEN
@Composable
fun SearchExploreScreen(viewModel: InstagramViewModel) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_screen")
    ) {
        // Search text box
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            prefix = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            placeholder = { Text("Search comments, creators, locations...", fontSize = 13.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("explore_search_bar"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InstaPink,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Missing results",
                        tint = Color.LightGray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No posts match your search query.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            // Visual Grid representation matching typical explore tab grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(posts) { post ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                // Simulate magnifying view or detail feed slide open!
                                viewModel.showCommentsForPost(post)
                            }
                    ) {
                        if (post.imageResId == 0 && post.gradientStartHex != null) {
                            val brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(android.graphics.Color.parseColor(post.gradientStartHex)),
                                    Color(android.graphics.Color.parseColor(post.gradientEndHex ?: "#ffffff"))
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(brush),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Text",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val filterApplied = post.caption.substringAfter("[Filter: ", "").substringBefore("]", "")
                            val colorFilter = remember(filterApplied) { getFilterMatrix(filterApplied) }

                            Image(
                                painter = painterResource(id = post.imageResId),
                                contentDescription = "Explore post clip",
                                contentScale = ContentScale.Crop,
                                colorFilter = colorFilter,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Little favorite overlay representing popular metrics
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f))
                                .padding(6.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = post.likesCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// CREATE POST SCREEN
@Composable
fun CreatePostScreen(viewModel: InstagramViewModel) {
    var captionText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }

    val templateIndex by viewModel.selectedTemplateIndex.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("create_post_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create New Post",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Photo Preview Canvas with Real Filter Blend Overlay
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val imageRes = viewModel.postTemplates[templateIndex]
                val colorFilter = remember(activeFilter) { getFilterMatrix(activeFilter) }

                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Live template preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )

                // Active Filter Name Overlay sticker
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "Filter: $activeFilter",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Template selector slider row
        Text(
            text = "Select Photo Template:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.postTemplates.forEachIndexed { idx, resId ->
                val selected = templateIndex == idx
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) InstaPink else Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.selectTemplateIndex(idx) }
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Template Choice $idx",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Active Filter picker scrolling row
        Text(
            text = "Apply Photo Filter:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.availableFilters) { filterName ->
                val selected = activeFilter == filterName
                Button(
                    onClick = { viewModel.selectFilter(filterName) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) InstaPink else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("filter_select_$filterName")
                ) {
                    Text(text = filterName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Caption TextField
        OutlinedTextField(
            value = captionText,
            onValueChange = { captionText = it },
            label = { Text("Write a caption...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("create_caption_input"),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InstaPink,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Location TextField
        OutlinedTextField(
            value = locationText,
            onValueChange = { locationText = it },
            label = { Text("Add Location") },
            prefix = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Pin",
                    modifier = Modifier.size(16.dp),
                    tint = InstaPink
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("create_location_input"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InstaPink,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Big Radiant Publish Button
        Button(
            onClick = {
                viewModel.publishNewPost(captionText, locationText)
                captionText = ""
                locationText = ""
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("publish_post_button")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(InstaPurple, InstaPink, InstaOrange)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Share to Feed",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// FILTER HELPER MATRIX BUILDER
fun getFilterMatrix(filterName: String): ColorFilter? {
    return when (filterName) {
        "Clarendon" -> {
            // Cool blue tones, high contrast
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 0f,
                        0f, 1.1f, 0f, 0f, 0f,
                        0f, 0f, 1.3f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        "Lark" -> {
            // Warm sunshine bright color booster
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.1f, 0f, 0f, 5f,
                        0f, 0f, 0.9f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        "Juno" -> {
            // Warm tones highlighted, red/neon glowing
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, 15f,
                        0f, 0.9f, 0f, 0f, -10f,
                        0f, 0f, 1.1f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        "Monochrome" -> {
            // Full desaturated black and white
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0.33f, 0.33f, 0.33f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        else -> null
    }
}

// REELS SCREEN
@Composable
fun ReelsScreen(viewModel: InstagramViewModel) {
    val reelsList by viewModel.reels.collectAsStateWithLifecycle()
    var activeReelIndex by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()

    if (reelsList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = InstaPink)
        }
    } else {
        // High fidelity fullscreen vertical layout representing reels
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("reels_screen")
        ) {
            val activeReel = reelsList[activeReelIndex]

            // Image display serving as simulated video backdrop
            Image(
                painter = painterResource(id = activeReel.imageResId),
                contentDescription = "Simulated Reel Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic diagonal color gradients overlay to simulate active neon backdrop lighting
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Right column controls panel overlay (likes, comments, share, music disc)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Like Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.toggleReelLike(activeReel.id) },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(46.dp)
                            .testTag("reel_like_${activeReel.id}")
                    ) {
                        Icon(
                            imageVector = if (activeReel.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Reel",
                            tint = if (activeReel.isLiked) HeartRed else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        activeReel.likesCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Comment Button trigger Dialogue
                var showCommentsOverlay by remember { mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showCommentsOverlay = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(46.dp)
                            .testTag("reel_comments_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        activeReel.commentsList.size.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (showCommentsOverlay) {
                    ReelsCommentsDialog(
                        reel = activeReel,
                        onSubmit = { viewModel.addReelComment(activeReel.id, it) },
                        onDismiss = { showCommentsOverlay = false }
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Rotating circular Vinyl track sticker description representation
                val infiniteTransition = rememberInfiniteTransition(label = "musicDiscRot")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "musicRotAnim"
                )

                Box(
                    modifier = Modifier
                        .rotate(rotation)
                        .size(38.dp)
                        .background(Color.DarkGray, CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Music Playing",
                        tint = InstaYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Left Bottom detail overlay (Author name, caption description, music ticker)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 20.dp, start = 14.dp, end = 74.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = activeReel.authorAvatarResId),
                        contentDescription = activeReel.authorName,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = activeReel.authorName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = activeReel.description,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Music Tag",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeReel.musicName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Top screen swap tabs simulation (swap between consecutive mock files)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                reelsList.forEachIndexed { index, _ ->
                    val active = activeReelIndex == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(50.dp)
                            .height(3.dp)
                            .background(
                                color = if (active) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { activeReelIndex = index }
                    )
                }
            }

            // Swipe Up / Down tip overlay indicator
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 34.dp, start = 14.dp, end = 14.dp)
            ) {
                Text("Reels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Button(
                    onClick = {
                        activeReelIndex = (activeReelIndex + 1) % reelsList.size
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Next Reel", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// STORY VIEWING IMMERSIVE OVERLAY
@Composable
fun StoryViewerOverlay(
    story: StoryEntity,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }

    // Start auto advance story progress timer
    LaunchedEffect(key1 = story.id) {
        progress = 0f
        while (progress < 1.0f) {
            delay(50)
            progress += 0.01f
        }
        onNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("story_viewer_overlay")
    ) {
        // Story image visual art central rendering
        Image(
            painter = painterResource(id = story.storyImageResId),
            contentDescription = "Active Story Media",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Backdrop tint darkening bottom and tops
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.5f))
                    )
                )
        )

        // Top horizontal progressive tick bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
        }

        // Creator header overlay details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = story.authorAvatarResId),
                contentDescription = story.authorName,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(story.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("2h ago", color = Color.LightGray, fontSize = 10.sp)
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close View",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Tap targets left side / right side to advance
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onPrev() }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onNext() }
            )
        }

        // Display story caption overlay
        if (story.caption.isNotBlank()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 90.dp, start = 20.dp, end = 20.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Simulated Direct story reply textbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Send quick reply...", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                readOnly = true, // Simulated
                trailingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Reply",
                        tint = Color.White
                    )
                }
            )
        }
    }
}

// PROFILE SCREEN
@Composable
fun ProfileScreen(viewModel: InstagramViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Count only user published posts (authorName == "you")
    val userPostsCount = remember(posts) {
        posts.count { it.authorName == "you" || it.authorName == "Alex Mercer" }
    }

    // Grid details list corresponding strictly to active feed uploads
    val userFeedList = remember(posts) {
        posts.filter { it.authorName == "you" || it.authorName == "Alex Mercer" }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_screen_layout"),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // Heading block stats row
        item {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar circle frame
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(2.dp, InstaPink, CircleShape)
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = profile.avatarResId),
                            contentDescription = "My Avatar Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Column grid holding post counts, followers count
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(number = userPostsCount.toString(), label = "Posts")
                        ProfileStatItem(number = profile.followersCount.toString(), label = "Followers")
                        ProfileStatItem(number = profile.followingCount.toString(), label = "Following")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bio Description Card Info
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${profile.username}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = InstaPink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.bio,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Edit Profile, Share stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditProfileDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("edit_profile_btn"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { /* Simulate sharing */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .width(50.dp)
                            .height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share details",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }

        // Toggle Grid headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "My Posts Grid",
                    tint = InstaPink,
                    modifier = Modifier.size(24.dp)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = InstaPink.copy(alpha = 0.5f))
        }

        // Grid contents display
        if (userFeedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Empty Grid",
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No posts shared yet. Share your first memory!",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Display user grid (Chunk list as rows of 3 to render columns)
            val chunked = userFeedList.chunked(3)
            items(chunked) { rowOfPosts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    rowOfPosts.forEachIndexed { index, post ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clickable { viewModel.showCommentsForPost(post) }
                        ) {
                            if (post.imageResId == 0) {
                                val brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(android.graphics.Color.parseColor(post.gradientStartHex ?: "#ffffff")),
                                        Color(android.graphics.Color.parseColor(post.gradientEndHex ?: "#ffffff"))
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(brush)
                                )
                            } else {
                                val filterApplied = post.caption.substringAfter("[Filter: ", "").substringBefore("]", "")
                                val colorFilter = remember(filterApplied) { getFilterMatrix(filterApplied) }

                                Image(
                                    painter = painterResource(id = post.imageResId),
                                    contentDescription = "My Grid Item",
                                    contentScale = ContentScale.Crop,
                                    colorFilter = colorFilter,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    // Empty weights blocks for incomplete grid rows
                    if (rowOfPosts.size < 3) {
                        repeat(3 - rowOfPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Edit profile dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onSave = { name, bio ->
                viewModel.updateProfile(name, bio)
                showEditProfileDialog = false
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }
}

// PROFILE STATS ITEM COLUMN
@Composable
fun ProfileStatItem(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// EDIT PROFILE POPUP DIALOGUE
@Composable
fun EditProfileDialog(
    profile: UserProfile,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(profile.name) }
    var editBio by remember { mutableStateOf(profile.bio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name")
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("About Bio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("edit_profile_bio")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editName, editBio) },
                colors = ButtonDefaults.buttonColors(containerColor = InstaPink),
                modifier = Modifier.testTag("submit_profile_edit")
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// SIMULATED DIRECT MESSAGES POPUP INBOX
@Composable
fun SimulatedInboxDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Inbox icon",
                    tint = InstaPink,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Direct Messages", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.height(280.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Direct messages are simulated offline. Tap on standard contacts to see messaging templates:", fontSize = 12.sp, color = Color.Gray)

                val contacts = listOf(
                    Triple("travel_explorer", "That ocean sun looks superb!", R.drawable.img_post_travel),
                    Triple("cafe_lover", "Let's catch up over coffee soon!", R.drawable.img_post_cafe),
                    Triple("neon_designer", "Thanks for viewing my digital artwork!", R.drawable.img_post_creative)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(contacts) { (username, lastMsg, resId) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "$username avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(lastMsg, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Close Inbox", color = MaterialTheme.colorScheme.surface)
            }
        }
    )
}

// REELS REPLIES DIALOGUE HELPER
@Composable
fun ReelsCommentsDialog(
    reel: ReelItem,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reel Thread Comments", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyColumn(
                    modifier = Modifier.height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (reel.commentsList.isEmpty()) {
                        item {
                            Text("No posts commented yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        items(reel.commentsList) { comment ->
                            Row {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("${comment.authorName} ")
                                        }
                                        append(comment.text)
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Add comments message...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_comment_input_box"),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(textInput)
                    textInput = ""
                    onDismiss()
                },
                enabled = textInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = InstaPink),
                modifier = Modifier.testTag("submit_reel_comment")
            ) {
                Text("Add Comment", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color.Gray)
            }
        }
    )
}

// INVERSION EXTENSION UTILITY FOR MATRICES DECORATIVE BLENDING (ROTATION ANGLE)
fun Modifier.rotate(degrees: Float): Modifier = this.drawBehind {
    drawContext.transform.rotate(degrees)
}
