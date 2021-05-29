
import top.limbang.doctor.client.MinecraftClient
import top.limbang.doctor.client.utils.ServiceInfoUtils


fun main() {
    val json = MinecraftClient.ping("mc.blackyin.xyz", 529).get()
    //val json = "{\"description\":{\"text\":\"A Minecraft Server\"},\"players\":{\"max\":20,\"online\":1,\"sample\":[{\"id\":\"67a3e3b5-16a4-4c47-8c4d-d4ef5a87382e\",\"name\":\"yuedu233_\"}]},\"version\":{\"name\":\"1.16.5\",\"protocol\":754}}"

    println(ServiceInfoUtils.getServiceInfo(json))
}