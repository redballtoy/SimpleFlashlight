package com.gmail.redballtoy.fonaric;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.app.AlertDialog;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.Switch;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SoundPool.OnLoadCompleteListener{

    private String TAG = "MyLog";
    private int sound;
    private SoundPool soundPool;
    private Camera camera;
    Camera.Parameters parameters;
    private Switch mySwitchOnOff;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: 6 вызов методов создания SoundPool через проверку версии устройства
        Log.d(TAG, "6");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createSoundPoolWithBuilder();
        } else {
            createSoudPoolConstructor();
        }


        //TODO: 7 слушатель на загрузку
        Log.d(TAG, "7");
        soundPool.setOnLoadCompleteListener(this);
        sound = soundPool.load(this, R.raw.click,1);
        //загрузка файлов происходит асинхронно и по его загрузке срабатывает
        //setOnLoadCompleteListener


        //TODO:1 определяем переключатель Switch и присваиваем ему слушатель
        Log.d(TAG, "1");
        mySwitchOnOff = (Switch) findViewById(R.id.sw_turn_on_off);
        //устанавливаем его в положение вкл
        mySwitchOnOff.setChecked(false);
        //Отмасштабируем его
        Float scaleSwitch = 3f;
        mySwitchOnOff.setScaleX(scaleSwitch);
        mySwitchOnOff.setScaleY(scaleSwitch);
        //присваиваем слушатель
        mySwitchOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setFlashLightOn();
                } else {
                    setFlashLightOff();
                }
            }
        });

        //TODO: 2 проверка наличия вспышки
        Log.d(TAG, "2");
        boolean isCameraFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        //обработка результата
        if (!isCameraFlash) {
            showCameraAlert();
        } else {
            camera = Camera.open();//получаем экземпляр класса камера
        }

    }

    //----------------------------onCreate()---------------------------------------

    //TODO: 3 Диалог выхода при отсутствии вспышки
    private void showCameraAlert() {
        Log.d(TAG, "3");
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_title)
                .setMessage(R.string.error_text)
                .setPositiveButton(R.string.exit_message, (dialog, which) -> {
                    finish();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //TODO: 4 Построение SoudPool для API 21 и выше
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createSoundPoolWithBuilder() {
        Log.d(TAG, "4");
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(1)
                .build();

    }

    //TODO: 5 Построение SoundPool для API 4.4 и выше
    @SuppressWarnings("depreciaton")
    protected void createSoudPoolConstructor(){
        Log.d(TAG, "5");
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        //1 - MaxStreams, STREAM_MUSIC - аудиопоток, 0 - показатель качества игнорируемый системой
    }

    //TODO: 8 Функция включения вспышки

    private void setFlashLightOn() {
        //Toast.makeText(this, "FlashOn", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "8");
        /*Проигрываем звук методом play
        attributes:
        1 - leftVolume (громкость левого канала),
        1 - rightVolume (громкость правого канала),
        0 - priority (приоритет),
        0 - loop (количество повторов),
        1 - rate (скорость воспроизведения)*/
        soundPool.play(sound, 1, 1, 0, 0, 1);

        //Использование отдельного потока что бы не перегружать интерфейс приложения

        new Thread(new Runnable() {
            @Override
            public void run() {
                //проверяем что камера создана
                if (camera != null) {
                    parameters = camera.getParameters();
                    //если параметры получены сохраняем их в список
                    //выясняем какой параметр вспышки использвать поскольку
                    //параметров два для разных произвожителей телефонов
                    //FLASH_MODE_TORCH или FLASH_MODE_ON

                    if (parameters != null) {
                        List supportedFlashModes = parameters.getSupportedFlashModes();

                        if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        } else camera = null;
                        if (camera != null) {
                            camera.setParameters(parameters);
                            //метод стартующий превью камеры, в данном случае нужен
                            //для включения вспышки
                            //работает на всех кроме Nexus 5
                            // для него надо вызывать SurfaceTexture(0)
                            camera.startPreview();
                            try {
                                camera.setPreviewTexture(new SurfaceTexture(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();// запуск потока
    }
    //TODO: 9 метод выключения вспышки
    private void setFlashLightOff() {
        Log.d(TAG, "9");
        //Toast.makeText(this, "FlashOff", Toast.LENGTH_SHORT).show();
        //проигрываем звук
        soundPool.play(sound, 1, 1, 0, 0, 1);
        //создаем новый поток
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (camera != null) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    camera.stopPreview();
                }
            }
        }).start();

    }

    //TODO: 10 Освобождение камеры для других приложений
    //запускается когда пользователь сворачивает данное приложение или
    //запускает другое, выполняется onStop поэтому переопределяем его
    private void releaseCamera() {
        Log.d(TAG, "10");
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stop");
        super.onStop();
        releaseCamera();
    }

    //освобождение камеры при переходе пользователя к другому экрану
    @Override
    protected void onPause() {
        Log.d(TAG, "Pause");
        super.onPause();
        releaseCamera();
        mySwitchOnOff.setChecked(false);

    }

    //при возвращеннии в приложение включаем вспышку и переключатель


    /*@Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        if (camera == null) {
            camera = Camera.open();
        } else {
            setFlashLightOn();
        }
        mySwitchOnOff.setChecked(true);
    }*/

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {

    }
}
