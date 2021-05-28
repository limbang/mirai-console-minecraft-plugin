package top.limbang.utils

import kotlinx.serialization.json.JsonPrimitive
import top.limbang.entity.ServiceInfo

object ServiceInfoUtil {

    fun getServiceInfo(json:String): ServiceInfo {
        val jsonPrimitive = JsonPrimitive(json)

        println(jsonPrimitive)
        println(jsonPrimitive.content)
        println(jsonPrimitive.isString)


        //val versionNumber = obj.getAsJsonObject("version").getAsJsonPrimitive("protocol").asInt
        //val versionName = AutoUtils.autoVersion(json)




        return ServiceInfo(

        )
    }


}