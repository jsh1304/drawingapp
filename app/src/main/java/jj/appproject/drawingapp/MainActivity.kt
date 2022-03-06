package jj.appproject.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get

class MainActivity : AppCompatActivity() {


    private var drawingView: DrawingView? = null

    private var mImageButtonCurrentPaint: ImageButton? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> = // 이미지 선택 위한 변수
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ // StartActivityForResult 요청 -> result를 가져온다.
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){ // [조건 1] resultCode = result의 종류, [조건 2] result 데이터가 비어있는지 여부
                val imageBackGround: ImageView = findViewById(R.id.iv_background)

                imageBackGround.setImageURI(result.data?.data) // .setImage = drawable, bitmap, resource 등을 설정 가능
                // result.data?(데이터 위치).data(값)
            }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> = // ActivityResultLauncher 사용시 어떤 종류의 launch인지 정의
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key // key = 권한 이름
                val isGranted = it.value // value = 권한의 승인 여부
                if(isGranted){
                    Toast.makeText(
                        this@MainActivity,
                        "접근이 승인되어 저장 파일을 불러올 수 있습니다.",
                        Toast.LENGTH_LONG
                    ).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)  // EXTERNAL_CONTENT_URI == 기기 안의 위치 <-- intent에서 가져옴.
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity,
                            "당신은 접근 승인을 거부하셨습니다.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palette_pressed)
        )

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibGallery : ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener{
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )){
            showRationaleDialog("Drawing App", "그림그림 앱은" +
                    "당신의 갤러리에 대한 접근을 승인해야합니다.")
        }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    //brush size 선택 method
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush 두께 : ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()

            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.palette_pressed
                )
            )

            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.palette_normal
                )
            )

            mImageButtonCurrentPaint = view
        }
    }


    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel"){dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View): Bitmap{

        // 뷰 크기로 비트 맵 정의
        // CreateBitmap : mutable bitmap을 특정 폭 높이와 함께 반환
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        // 캔버스에 바인딩
        val canvas = Canvas(returnedBitmap)

        // 뷰의 background를 get
        val bgDrawable = view.background
        if(bgDrawable != null){
            // background drawable 존재 -> 캔버스에 그림 그린다.
            bgDrawable.draw(canvas)
        }
        else{
            // background drawable 존재x -> 캔버스를 하얀 background로 구성
            canvas.drawColor(Color.WHITE)
        }

        // canvas에 view를 그림
        view.draw(canvas)

        return returnedBitmap
 
    }




}
