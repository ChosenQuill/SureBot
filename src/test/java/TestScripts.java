import com.suredroid.discord.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.HashMap;
import java.util.Optional;

public class TestScripts {
    //@Test
    public void testM (){
        HashMap<String, String> langs = new HashMap<>();
        Optional<HashMap<String, String>> gotLangs = CommonUtils.getInternalJson("/files/langs.json",langs);
        assert(gotLangs.isPresent());
        System.out.println(gotLangs.get());
    }

    @Test
    public void AnnotationTest(){
        noNulls(null);
    }


    public void noNulls(@NotNull String something){

    }

}
