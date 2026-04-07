package com.gkim.im.android.core.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.gkim.im.android.core.model.AttachmentType
import com.gkim.im.android.core.model.MediaInput

class MediaPickerController(
    val pickImage: () -> Unit,
    val pickVideo: () -> Unit,
)

@Composable
fun rememberMediaPickerController(onMediaSelected: (MediaInput) -> Unit): MediaPickerController {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onMediaSelected(MediaInput(AttachmentType.Image, uri))
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onMediaSelected(MediaInput(AttachmentType.Video, uri))
        }
    }

    return remember(imagePicker, videoPicker) {
        MediaPickerController(
            pickImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            pickVideo = {
                videoPicker.launch(arrayOf("video/*"))
            },
        )
    }
}
