package com.twilio.conversations.app.ui.dialogs

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.core.content.FileProvider
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.applicationContext
import com.twilio.conversations.app.common.extensions.getString
import com.twilio.conversations.app.common.extensions.lazyActivityViewModel
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.databinding.DialogAttachFileBinding
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AttachFileDialog : BaseBottomSheetDialogFragment() {

    lateinit var binding: DialogAttachFileBinding

    var imageCaptureUri = Uri.EMPTY

    val messageListViewModel by lazyActivityViewModel {
        val conversationSid = requireArguments().getString(ARGUMENT_CONVERSATION_SID)!!
        injector.createMessageListViewModel(applicationContext, conversationSid)
    }

    private val takePicture = registerForActivityResult(TakePicture()) { success ->
        if (success) sendMediaMessage(imageCaptureUri)
        dismiss()
    }

    private val openDocument = registerForActivityResult(OpenDocument()) { uri: Uri? ->
        uri?.let { sendMediaMessage(it) }
        dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { imageCaptureUri = it.getParcelable(IMAGE_CAPTURE_URI) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogAttachFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            binding.takePhoto.visibility = View.GONE
        }

        binding.takePhoto.setOnClickListener {
            startImageCapture()
        }

        binding.fileManager.setOnClickListener {
            openDocument.launch(arrayOf("*/*"))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IMAGE_CAPTURE_URI, imageCaptureUri)
    }

    private fun startImageCapture() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", picturesDir)
        imageCaptureUri =
            FileProvider.getUriForFile(requireContext(), "com.twilio.conversations.app.fileprovider", photoFile)
        Timber.d("Capturing image to $imageCaptureUri")

        takePicture.launch(imageCaptureUri)
    }

    fun sendMediaMessage(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val type = contentResolver.getType(uri)
        val name = contentResolver.getString(uri, OpenableColumns.DISPLAY_NAME)
        if (inputStream != null) {
            messageListViewModel.sendMediaMessage(uri.toString(), inputStream, name, type)
        } else {
            messageListViewModel.onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            Timber.w("Could not get input stream for file reading: $uri")
        }
    }


    companion object {

        private const val IMAGE_CAPTURE_URI = "IMAGE_CAPTURE_URI"

        private const val ARGUMENT_CONVERSATION_SID = "ARGUMENT_CONVERSATION_SID"

        fun getInstance(conversationSid: String) = AttachFileDialog().apply {
            arguments = Bundle().apply {
                putString(ARGUMENT_CONVERSATION_SID, conversationSid)
            }
        }
    }
}
