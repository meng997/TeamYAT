package sg.gowild.sademo;
import android.app.Service;
import android.content.Intent;
public abstract class AbstractService extends Service{
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
