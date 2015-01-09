import java.util.Arrays;

/**
 * Created by Yoni on 29/12/2014.
 */
public enum EHttpMethods {
    GET, POST, TRACE, OPTIONS, HEAD;

    public static String asListString(){
        EHttpMethods[] methodsArray = EHttpMethods.values();
        String methodsList = Arrays.toString(methodsArray);
        return methodsList.substring(1, methodsList.length() - 1);
    }
}
