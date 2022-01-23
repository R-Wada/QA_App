package jp.techacademy.yoshihisa.wada.qa_app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_send.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class QuestionDetailActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener  {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavRef: DatabaseReference
    private var FavState: Boolean = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }
        val user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        if (user == null) {
            imageView3.setVisibility(View.INVISIBLE)
        } else {
            imageView3.setOnClickListener(this)
            mFavRef = dataBaseReference.child(FavPATH).child(user!!.uid)
            mFavRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (h in dataSnapshot.children){
                        if (h.key == mQuestion.questionUid) {
                            FavState = true
                        }
                    }
                    if (FavState){
                        imageView3.setImageResource(R.drawable.ic_star)
                    }else{
                        imageView3.setImageResource(R.drawable.ic_star_border)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    // ...
                }

            })
        }
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

    }

    override fun onClick(v: View?) {
        if (v === imageView3) {
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            if (FavState){
                imageView3.setImageResource(R.drawable.ic_star_border)
                val favRef = dataBaseReference.child(FavPATH).child(uid).child(mQuestion.questionUid.toString())
                favRef.removeValue()
                FavState = false
                Toast.makeText(applicationContext, "お気に入りから削除", Toast.LENGTH_SHORT).show()
            }else{
                val favRef = dataBaseReference.child(FavPATH).child(uid).child(mQuestion.questionUid.toString())
                val data = HashMap<String, String>()
                data["genre"] = mQuestion.genre.toString() ?: ""
                favRef.setValue(data)
                FavState = true
                imageView3.setImageResource(R.drawable.ic_star)
                Toast.makeText(applicationContext, "お気に入りへ登録", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        //progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
        }
    }
}