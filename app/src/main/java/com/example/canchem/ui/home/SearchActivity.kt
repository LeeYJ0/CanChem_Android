package com.example.canchem

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.canchem.databinding.ActivitySearchBinding
import androidx.appcompat.widget.SearchView
import android.text.InputFilter
import android.widget.EditText
import android.widget.Toast

class SearchActivity : AppCompatActivity() {
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
    }
}