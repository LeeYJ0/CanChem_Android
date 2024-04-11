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
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build

class SearchActivity : AppCompatActivity() {

    //카메라 앱으로 사진을 촬영하기 위한 요청 코드 (결과를 식별하는데 사용)
    companion object{
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_CAMERA_PERMISSION = 2
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Android 10 이상일 때는 10 이상의 퍼미션을 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestCameraPermissionAndroidQAndAbove()
        }
        // Android 13 이상일 때는 13 이상의 퍼미션을 요청
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestCameraPermissionAndroidMAndAbove()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // EditText 가져오기
        val editText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        // 입력 필터링 설정
        editText.filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val allowedChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ=()-#.$:/\\"
            val filteredStringBuilder = StringBuilder()
            // 입력된 문자 중에서 허용된 문자인지 확인
            for (i in start until end) {
                if (allowedChars.contains(source[i])) {
                    filteredStringBuilder.append(source[i])
                }
                else{
                    //허용되지 않는 문자가 입력되었을 때 토스트 메시지 표시
                    Toast.makeText(this, "잘못된 입력 방식입니다.", Toast.LENGTH_SHORT).show()
                }
            }
            // 필터링된 문자열 반환
            filteredStringBuilder.toString()
        })

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchView.maxWidth = Int.MAX_VALUE
        searchView.isSubmitButtonEnabled = true

        // 검색 버튼 클릭 이벤트 처리
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 검색 버튼이 클릭되었을 때 실행됩니다.
                // 여기서는 ApiActivity로의 인텐트 전환을 수행합니다.
                val intent = Intent(this@SearchActivity, ApiActivity::class.java)
                // 검색어를 인텐트에 추가할 수도 있습니다.
                intent.putExtra("search_query", query)
                startActivity(intent)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 검색어가 변경될 때 실행됩니다.
                return false
            }
        })

        //카메라 버튼 클릭시 카메라 앱 시행
        val cameraButton = findViewById<ImageButton>(R.id.cameraButton)
        cameraButton.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                Toast.makeText(this, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //카메라 앱이 종료되고 다시 검색 액티비티로 돌아왔을 때 호출
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            // 여기서 촬영한 이미지를 처리하거나 표시할 수 있음.
            Toast.makeText(this, "사진을 찍었습니다!", Toast.LENGTH_SHORT).show()
        }
    }

    // 안드로이드 10 이상에서 카메라 퍼미션을 요청
    private fun requestCameraPermissionAndroidQAndAbove() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    // 안드로이드 13 이상에서 카메라 퍼미션을 요청
    private fun requestCameraPermissionAndroidMAndAbove() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }
}