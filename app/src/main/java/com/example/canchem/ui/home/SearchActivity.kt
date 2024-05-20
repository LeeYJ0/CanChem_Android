package com.example.canchem

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.canchem.databinding.ActivitySearchBinding
import androidx.appcompat.widget.SearchView
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.app.AlertDialog
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.soundcloud.android.crop.Crop
import java.io.ByteArrayOutputStream
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class SearchActivity : AppCompatActivity() {
    companion object{
        //카메라 앱을 사진 찍기 위한 요청 코드
        const val REQUEST_IMAGE_CAPTURE = 1
        //카메라 앱의 권한을 위한 요청 코드
        const val REQUEST_CAMERA_PERMISSION = 2
        // 갤러리 앱에서 이미지를 선택하기 위한 요청 코드
        const val REQUEST_IMAGE_PICK = 3
        //갤러리 앱의 권한을 위한 요청 코드
        const val REQUEST_GALLERY_PERMISSION =4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //입력 필터링 적용
        val editText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        setInputFilter(editText)

        binding.searchView.maxWidth = Int.MAX_VALUE
        binding.searchView.isSubmitButtonEnabled = true

        // 검색 버튼 클릭 이벤트 처리
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 검색 버튼이 클릭되었을 때 실행
                // ApiActivity로의 인텐트 전환을 수행
                val intent = Intent(this@SearchActivity, ApiActivity::class.java)
                // 검색어를 인텐트에 추가.
                intent.putExtra("search_query", query)
                startActivity(intent)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // 검색어가 변경될 때 실행
                return false
            }
        })

        // 카메라 버튼 클릭시
        binding.cameraButton.setOnClickListener {
            //카메라 권한이 있는지 확인
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                //카메라 앱 실행
                startCamera()
            }else{
                //카메라 권한 요청
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            }
        }

        // 갤러리 버튼 클릭시
        binding.galleryButton.setOnClickListener {
            val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickPhotoIntent.type = "image/*"
            val sdkVersion = Build.VERSION.SDK_INT
            if (pickPhotoIntent.resolveActivity(packageManager) != null) {
                // Android 13이상 버전 퍼미션을 요청
                if (sdkVersion >= 33) {
                    requestGalleryPermission13(pickPhotoIntent)
                }
                // Android 13이전 버전 퍼미션을 요청
                else {
                    requestGalleryPermission10(pickPhotoIntent)
                }
            }
            else {
                Toast.makeText(this, "갤러리 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // 카메라, 갤러리, 크롭 등의 기능을 수행 했을 때
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "취소 되었습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        when (requestCode) {
            // 갤러리에서 이미지를 선택한 경우
            REQUEST_IMAGE_PICK -> {
                val photoUri = data?.data
//                photoUri?.let{showConfirmationDialog(it)}
                photoUri?.let { cropImage(it) }
            }
            // 카메라에서 이미지를 선택한 경우
            REQUEST_IMAGE_CAPTURE -> {
                val photoUri = imageUri

                photoUri?.let { cropImage(it) }
            }
            // 이미지를 크롭 한 후
            Crop.REQUEST_CROP -> {
                val result = Crop.getOutput(data)
                result?.let { croppedUri ->
                    // 크롭된 이미지의 Uri를 통해 Bitmap으로 변환
                    val croppedBitmap =
                        MediaStore.Images.Media.getBitmap(contentResolver, croppedUri)
                    // Bitmap을 Base64로 변환
                    val base64Image = bitmapToBase64(croppedBitmap)
                    // 서버로 업로드 등 작업 수행
                }
            }
        }
    }

    // 입력 필터링 적용 함수
    fun setInputFilter(editText: EditText) {
        val allowedChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ=()-#.$:/\\"
        editText.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val filteredStringBuilder = StringBuilder()
            // 입력된 문자 중에서 허용된 문자인지 확인
            for (i in start until end) {
                if (allowedChars.contains(source[i])) {
                    filteredStringBuilder.append(source[i])
                } else {
                    // 허용되지 않는 문자가 입력되었을 때 토스트 메시지 표시
                    Toast.makeText(editText.context, "잘못된 입력 방식입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            // 필터링된 문자열 반환
            filteredStringBuilder.toString()
        })
    }

    // 카메라로 촬영된 이미지의 파일 경로를 저장하는 프로퍼티
    private var imageUri: Uri? = null

    //카메라 앱 실행 함수
    private fun startCamera(){
        val state = Environment.getExternalStorageState()
        // 외장 메모리 검사
        if (Environment.MEDIA_MOUNTED == state) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                var photoFile: File? = null

                try {
                    photoFile = createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "이미지 처리 오류. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                if (photoFile != null) {
                    val providerURI = FileProvider.getUriForFile(this, "com.example.canchem.fileprovider", photoFile)
                    imageUri = providerURI

                    // 파일 URI가 올바르게 설정되었는지 토스트 메시지로 확인
//                    if (providerURI != null) {
//                        Toast.makeText(this, "File URI: $providerURI", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this, "Failed to get file URI", Toast.LENGTH_SHORT).show()
//                    }

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
            else{
                Toast.makeText(this, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "외장 메모리를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라로 찍은 사진을 저장할 임시파일 생성 함수
    private fun createImageFile(): File? {
        // 이미지 파일 이름 생성
        val timeStamp = SimpleDateFormat("yyyyMMdd").format(Date())
        val imageFileName = "CanChem_${timeStamp}.jpg" //ex) CanChem_20240509.jpg

        var imageFile: File? = null
        val storageDir = File(
            Environment.getExternalStorageDirectory().toString() + "/Pictures",
            "pic"
        )

        // 저장 디렉토리가 존재하지 않으면 생성
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        imageFile = File(storageDir, imageFileName)

        if(imageFile.exists()){
            //파일을 프로그램 종료시 삭제
            imageFile.deleteOnExit()
        }

//        // 파일이 생성되었는지 토스트 메시지로 확인
//        if (imageFile.exists()) {
//            Toast.makeText(this, "File created at: ${imageFile.absolutePath}", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show()
//        }

        return imageFile
    }

    // 안드로이드 13 이전 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission10(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_GALLERY_PERMISSION
            )
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    // 안드로이드 13 이상 버전 갤러리 퍼미션을 요청
    private fun requestGalleryPermission13(intent: Intent) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                REQUEST_GALLERY_PERMISSION
            )
        } else {
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
        }
    }

    // 이미지를 Crop 화면으로 보내주는 함수
    private fun cropImage(photoUri: Uri) {
        // 임시 파일로 저장할 파일 객체 생성
        val savingUri = Uri.fromFile(createTempFile("cropImage", ".jpg"))
        //Crop.of(photoUri, savingUri).withAspect(1, 1).start(this) 1대1 비율로 크롭
        //크롭 수정 사용자 지정
        Crop.of(photoUri, savingUri).start(this)

    }

    // Bitmap을 Base64로 변환하는 함수
    private fun bitmapToBase64(croppedBitmap: Bitmap?): String {
        croppedBitmap?.let {
            val byteArrayOutputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return ""
    }

    // 퍼미션 요청 결과 처리
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CAMERA_PERMISSION ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "카메라 권한을 허용하였습니다.", Toast.LENGTH_SHORT).show()

                }
                //권한을 거절했을 경우
                else{
                    // 권한 설정을 위한 다이얼로그
                    goSettingActivityAlertDialog()
                }
            }
            REQUEST_GALLERY_PERMISSION ->{
                //13이전버전
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "갤러리 권한을 허용하였습니다.", Toast.LENGTH_SHORT).show()
                }
                //13버전
                else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "갤러리 권한을 허용하였습니다.", Toast.LENGTH_SHORT).show()
                }
                else{
                    goSettingActivityAlertDialog()
                }
            }
        }
    }

    //설정 -> 권한으로 이동하는 다이얼로그
    private fun goSettingActivityAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("허용 되지 않은 권한이 있습니다.")
            .setMessage("일부 기능이 제한 될 수 있습니다.\n설정에서 권한을 허용해주세요.\n권한 -> 저장공간 -> 허용")
            .setPositiveButton("허용하러 가기") { _, _ ->
                val goSettingPermission = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                goSettingPermission.data = Uri.parse("package:$packageName")
                startActivity(goSettingPermission)
            }
            .setNegativeButton("취소") { _, _ -> }
            .create()
            .show()
    }
//        //사진 선택 확인 다이얼로그 표시 함수
//        private fun showConfirmationDialog(photoUri: Uri) {
//            val alertDialogBuilder = AlertDialog.Builder(this)
//            alertDialogBuilder.apply {
//                setMessage("선택한 사진을 사용하시겠습니까?")
//                setPositiveButton("확인") { dialog, which ->
//                    // 확인 버튼을 누르면 크롭 화면으로 이동
//                    cropImage(photoUri)
//                }
//                setNegativeButton("취소") { dialog, which ->
//                    // 취소 버튼을 누르면 다이얼로그를 닫고 다시 갤러리 실행
//                    dialog.dismiss()
//                }
//            }
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
//        }

}
