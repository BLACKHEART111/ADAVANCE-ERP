package com.pocketdev.advance_erp.feature.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.R
import com.pocketdev.advance_erp.core.designsystem.ERPDivider
import com.pocketdev.advance_erp.core.designsystem.FullScreenImageDialog
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusError
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.designsystem.TextPrimary
import com.pocketdev.advance_erp.core.designsystem.TextSecondary
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel

data class GalleryItemDraft(
    val id: String,
    val uriString: String,
    val isPrimary: Boolean = false,
    val sortOrder: Int = 0
)

@Composable
fun PhotoGalleryPicker(
    title: String,
    photos: List<GalleryItemDraft>,
    onPhotosChanged: (List<GalleryItemDraft>) -> Unit,
    modifier: Modifier = Modifier,
    maxPhotos: Int = 10
) {
    val context = LocalContext.current
    var previewUri by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<GalleryItemDraft?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val updated = photos.toMutableList()
            for (uri in uris) {
                if (updated.size < maxPhotos) {
                    val isFirst = updated.isEmpty()
                    updated.add(
                        GalleryItemDraft(
                            id = "draft-${System.nanoTime()}-${updated.size}",
                            uriString = uri.toString(),
                            isPrimary = isFirst,
                            sortOrder = updated.size
                        )
                    )
                }
            }
            onPhotosChanged(updated)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title (${photos.size}/$maxPhotos)",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )

            if (photos.size < maxPhotos) {
                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photos",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Photos",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, ERPDivider, RoundedCornerShape(12.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tap to select photos from gallery",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(photos, key = { _, item -> item.id }) { index, item ->
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(
                                width = if (item.isPrimary) 2.dp else 1.dp,
                                color = if (item.isPrimary) SecondaryTeal else ERPDivider,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Thumbnail Preview
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { previewUri = item.uriString }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rememberImageModel(item.uriString))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Photo $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (item.isPrimary) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .background(SecondaryTeal, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PRIMARY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Controls: Reorder Left, Primary, Reorder Right, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move Left
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val reordered = photos.toMutableList()
                                            val temp = reordered[index]
                                            reordered[index] = reordered[index - 1]
                                            reordered[index - 1] = temp
                                            onPhotosChanged(reordered.mapIndexed { idx, draft -> draft.copy(sortOrder = idx) })
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Move Left",
                                        tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Toggle Primary
                                IconButton(
                                    onClick = {
                                        val updated = photos.mapIndexed { idx, draft ->
                                            draft.copy(isPrimary = idx == index)
                                        }
                                        onPhotosChanged(updated)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isPrimary) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Set Primary",
                                        tint = if (item.isPrimary) SecondaryTeal else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Move Right
                                IconButton(
                                    onClick = {
                                        if (index < photos.size - 1) {
                                            val reordered = photos.toMutableList()
                                            val temp = reordered[index]
                                            reordered[index] = reordered[index + 1]
                                            reordered[index + 1] = temp
                                            onPhotosChanged(reordered.mapIndexed { idx, draft -> draft.copy(sortOrder = idx) })
                                        }
                                    },
                                    enabled = index < photos.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Move Right",
                                        tint = if (index < photos.size - 1) TextSecondary else TextMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { photoToDelete = item },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Photo",
                                        tint = StatusError,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before deleting photo
    if (photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to remove this photo from the gallery?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toRemove = photoToDelete
                        photoToDelete = null
                        if (toRemove != null) {
                            val updated = photos.filter { it.id != toRemove.id }.toMutableList()
                            if (toRemove.isPrimary && updated.isNotEmpty()) {
                                updated[0] = updated[0].copy(isPrimary = true)
                            }
                            onPhotosChanged(updated.mapIndexed { idx, draft -> draft.copy(sortOrder = idx) })
                        }
                    }
                ) {
                    Text("Delete", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Full screen preview dialog
    if (previewUri != null) {
        FullScreenImageDialog(
            imageUriOrPath = previewUri!!,
            onDismiss = { previewUri = null }
        )
    }
}
