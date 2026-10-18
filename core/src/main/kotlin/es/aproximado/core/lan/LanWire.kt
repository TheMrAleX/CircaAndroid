package es.aproximado.core.lan

import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.KeyPairGenerator
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

sealed interface LanMessage {
    data class Hello(val token: String,val name: String,val liveRuler: Boolean = true): LanMessage
    data class State(val snapshot: LanSnapshot): LanMessage
    data class Submit(val game: String,val round: Int,val value: Double): LanMessage
    data class Preview(val game: String,val round: Int,val revision: Long,val value: Double): LanMessage
    data class Preference(val enabled: Boolean): LanMessage
    data class Live(val frame: LanLiveFrame): LanMessage
    data class Error(val reason: LanFailure): LanMessage
    data object Ping: LanMessage
    data object Pong: LanMessage
    data object Bye: LanMessage
}
object LanWire {
    const val VERSION=2
    const val MAX_FRAME=65536
    private fun DataOutputStream.str(s: String) { val b=s.toByteArray(Charsets.UTF_8);require(b.size<=2048);writeInt(b.size);write(b) }
    private fun DataInputStream.str(): String { val n=readInt();require(n in 0..2048);val b=ByteArray(n);readFully(b);return Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(b)).toString() }
    private fun DataInputStream.count(max: Int)=readInt().also { require(it in 0..max) }
    private fun DataOutputStream.prompt(q: LanPrompt) {
        str(q.id);str(q.category);str(q.text);str(q.unit ?: "");writeDouble(q.min);writeDouble(q.max);str(q.scale);writeInt(q.difficulty);writeInt(q.year ?: 0);writeInt(q.decimals)
    }
    private fun DataInputStream.prompt(): LanPrompt {
        val p=LanPrompt(str(),str(),str(),str().ifEmpty { null },readDouble(),readDouble(),str(),readInt(),readInt().takeIf { it!=0 },readInt())
        require(p.id.length<=80 && p.text.length<=512 && p.min.isFinite() && p.max.isFinite() && p.min>=0 && p.max>p.min && p.scale in listOf("linear","log") && (p.scale!="log" || p.min>0) && p.difficulty in 1..3 && p.decimals in 0..6)
        return p
    }
    fun encode(message: LanMessage): ByteArray {
        val bytes=ByteArrayOutputStream()
        DataOutputStream(bytes).use { w -> with(w) {
            when(message) {
                is LanMessage.Hello -> { writeByte(1);str(message.token);str(message.name);writeBoolean(message.liveRuler) }
                is LanMessage.Submit -> { writeByte(2);str(message.game);writeInt(message.round);writeDouble(message.value) }
                is LanMessage.Error -> { writeByte(3);writeInt(message.reason.ordinal) }
                LanMessage.Ping -> writeByte(4)
                LanMessage.Pong -> writeByte(5)
                LanMessage.Bye -> writeByte(6)
                is LanMessage.Preview -> { writeByte(8);str(message.game);writeInt(message.round);writeLong(message.revision);writeDouble(message.value) }
                is LanMessage.Preference -> { writeByte(9);writeBoolean(message.enabled) }
                is LanMessage.Live -> { writeByte(10);val f=message.frame;str(f.game);writeInt(f.round);writeLong(f.revision);writeInt(f.values.size);f.values.forEach { (id,v) -> writeInt(id);writeDouble(v) } }
                is LanMessage.State -> {
                    writeByte(7);val s=message.snapshot
                    str(s.roomId);str(s.gameId);writeLong(s.revision);writeInt(s.self);writeInt(s.stage.ordinal)
                    writeInt(s.members.size);s.members.forEach { writeInt(it.id);str(it.name);writeBoolean(it.online);writeBoolean(it.submitted) }
                    writeInt(s.rounds);writeInt(s.difficulty ?: 0);writeInt(s.categories.size);s.categories.sorted().forEach { str(it) }
                    writeInt(s.round);writeBoolean(s.prompt!=null);s.prompt?.let { prompt(it) };writeBoolean(s.mine!=null);s.mine?.let { writeDouble(it) }
                    writeInt(s.results.size);s.results.forEach { r -> prompt(r.prompt);writeDouble(r.answer);str(r.source);writeInt(r.guesses.size);r.guesses.forEach { writeDouble(it) } };writeBoolean(s.liveEnabled)
                }
            }
        } }
        return bytes.toByteArray().also { require(it.size<=MAX_FRAME-16) }
    }
    fun decode(bytes: ByteArray): LanMessage {
        require(bytes.size<=MAX_FRAME)
        val r=DataInputStream(ByteArrayInputStream(bytes))
        val message=with(r) { when(readUnsignedByte()) {
            1 -> LanMessage.Hello(str(),str(),readBoolean()).also { LanRoom.requireToken(it.token);LanRoom.cleanName(it.name) }
            2 -> LanMessage.Submit(str(),readInt(),readDouble()).also { require(it.game.length<=64 && it.round in 0..19 && it.value.isFinite()) }
            3 -> LanMessage.Error(LanFailure.entries[readInt()])
            4 -> LanMessage.Ping
            5 -> LanMessage.Pong
            6 -> LanMessage.Bye
            7 -> {
                val room=str();val game=str();val revision=readLong();val self=readInt();val stage=LanStage.entries[readInt()]
                val members=List(count(8)) { LanMember(readInt(),str(),readBoolean(),readBoolean()) }
                val rounds=readInt();val diff=readInt().takeIf { it!=0 };val cats=List(count(32)) { str() }.toSet();val round=readInt()
                val prompt=if(readBoolean()) prompt() else null;val mine=if(readBoolean()) readDouble() else null
                val results=List(count(20)) { LanRound(prompt(),readDouble(),str(),List(count(8)) { readDouble() }) }
                val live=readBoolean()
                require(!live || members.size in 2..3 && members.all { it.online })
                require(room.length<=64 && game.length<=64 && revision>=0 && members.size in 1..8 && self in members.indices && members.map { it.id }==members.indices.toList() && members.all { it.name.isNotBlank() && it.name.length<=12 } && rounds in listOf(5,10,15,20) && (diff==null || diff in 1..3) && round in 0 until rounds)
                require(stage==LanStage.LOBBY || members.size>=2 && prompt!=null)
                require(results.all { it.guesses.size==members.size && it.answer.isFinite() && it.answer in it.prompt.min..it.prompt.max && it.guesses.all { v -> es.aproximado.core.EstimateInput.accepts(it.prompt.model(),v) } })
                require(mine==null || prompt!=null && es.aproximado.core.EstimateInput.accepts(prompt.model(),mine))
                require(results.size==(when(stage) { LanStage.LOBBY -> 0;LanStage.ANSWERING -> round;else -> round+1 }))
                LanMessage.State(LanSnapshot(room,game,revision,self,stage,members,rounds,diff,cats,round,prompt,mine,results,live))
            }
            8 -> LanMessage.Preview(str(),readInt(),readLong(),readDouble()).also { require(it.game.length<=64 && it.round in 0..19 && it.revision>=0 && it.value.isFinite()) }
            9 -> LanMessage.Preference(readBoolean())
            10 -> {
                val game=str();val round=readInt();val revision=readLong();val pairs=List(count(2)) { readInt() to readDouble() }
                require(game.length<=64 && round in 0..19 && revision>=0 && pairs.all { it.first in 0..7 && it.second.isFinite() } && pairs.map { it.first }.distinct().size==pairs.size)
                LanMessage.Live(LanLiveFrame(game,round,revision,pairs.toMap()))
            }
            else -> throw IOException("Unknown message")
        } }
        require(r.available()==0);return message
    }
}

/** Ephemeral P-256 ECDH, code-authenticated transcript, HKDF-SHA256 and ordered AES-GCM frames.
 * Code holders are trusted room participants; passive knowledge of the code does not reveal other links.
 */
class LanChannel private constructor(private val input: DataInputStream,private val output: DataOutputStream,private val key: SecretKeySpec,private val client: Boolean) {
    private var sent=0L
    private var received=0L
    private fun crypt(mode: Int,data: ByteArray,counter: Long,direction: Int): ByteArray {
        require(counter>=0)
        val iv=ByteBuffer.allocate(12).putInt(direction).putLong(counter).array()
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(mode,key,GCMParameterSpec(128,iv));updateAAD("Aproximado-LAN-${LanWire.VERSION}".toByteArray());doFinal(data)
        }
    }
    @Synchronized fun send(message: LanMessage) {
        val data=crypt(Cipher.ENCRYPT_MODE,LanWire.encode(message),sent++,if(client) 1 else 2)
        output.writeInt(data.size);output.write(data);output.flush()
    }
    fun receive(): LanMessage {
        val size=input.readInt();require(size in 17..LanWire.MAX_FRAME)
        val bytes=ByteArray(size);input.readFully(bytes)
        return LanWire.decode(crypt(Cipher.DECRYPT_MODE,bytes,received++,if(client) 2 else 1))
    }
    companion object {
        private const val MAGIC=0x41505258
        fun open(input: InputStream,output: OutputStream,code: String,client: Boolean): LanChannel {
            require(Regex("[0-9]{8}").matches(code))
            val i=DataInputStream(input);val o=DataOutputStream(output)
            val salt=ByteArray(16)
            val pair=KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
            fun readPublic(): ByteArray {
                val size=i.readInt();require(size in 64..256)
                return ByteArray(size).also { i.readFully(it) }
            }
            fun sendPublic() { val public=pair.public.encoded;o.writeInt(public.size);o.write(public);o.flush() }
            val serverPublic: ByteArray
            val clientPublic: ByteArray
            if(client) {
                if(i.readInt()!=MAGIC || i.readInt()!=LanWire.VERSION) throw LanException(LanFailure.VERSION)
                i.readFully(salt);serverPublic=readPublic();clientPublic=pair.public.encoded;sendPublic()
            } else {
                SecureRandom().nextBytes(salt);o.writeInt(MAGIC);o.writeInt(LanWire.VERSION);o.write(salt);sendPublic()
                serverPublic=pair.public.encoded;clientPublic=readPublic()
            }
            val spec=PBEKeySpec(code.toCharArray(),salt,120000,256)
            val psk=try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
            fun hmac(key: ByteArray,data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key,"HmacSHA256"));doFinal(data) }
            val transcript="Aproximado-LAN-${LanWire.VERSION}".toByteArray()+salt+serverPublic+clientPublic
            fun proof(label: String)=hmac(psk,label.toByteArray()+transcript)
            fun verify(expected: ByteArray) { val received=ByteArray(32);i.readFully(received);if(!MessageDigest.isEqual(expected,received)) throw LanException(LanFailure.WRONG_CODE) }
            if(client) { o.write(proof("client"));o.flush();verify(proof("server")) }
            else { verify(proof("client"));o.write(proof("server"));o.flush() }
            val remote=KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(if(client) serverPublic else clientPublic)) as ECPublicKey
            val own=pair.public as ECPublicKey
            require(remote.params.curve==own.params.curve && remote.params.generator==own.params.generator && remote.params.order==own.params.order && remote.params.cofactor==own.params.cofactor)
            val shared=KeyAgreement.getInstance("ECDH").run { init(pair.private);doPhase(remote,true);generateSecret() }
            val extracted=hmac(psk,shared)
            val key=hmac(extracted,transcript+"AES-256-GCM".toByteArray()+byteArrayOf(1))
            psk.fill(0);shared.fill(0);extracted.fill(0)
            return LanChannel(i,o,SecretKeySpec(key,"AES"),client)
        }
    }
}

/** Numeric RFC1918/link-local endpoints only: never DNS, Internet servers or URL parsing. */
data class LanEndpoint(val host: String,val port: Int) {
    val address get()=InetSocketAddress(InetAddress.getByAddress(host.split('.').map { it.toInt().toByte() }.toByteArray()),port)
    override fun toString()="$host:$port"
    companion object {
        fun parse(raw: String,allowLoopback: Boolean = false): LanEndpoint {
            val parts=raw.trim().split(':');require(parts.size==2)
            val octets=parts[0].split('.');require(octets.size==4 && octets.all { it.matches(Regex("[0-9]{1,3}")) && it.toInt() in 0..255 })
            val ip=octets.joinToString(".") { it.toInt().toString() };val a=InetAddress.getByAddress(octets.map { it.toInt().toByte() }.toByteArray())
            require(a.isSiteLocalAddress || a.isLinkLocalAddress || allowLoopback && a.isLoopbackAddress)
            val port=parts[1].toInt();require(port in 1024..65535);return LanEndpoint(ip,port)
        }
    }
}
