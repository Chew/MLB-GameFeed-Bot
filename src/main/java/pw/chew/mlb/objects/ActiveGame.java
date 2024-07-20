package pw.chew.mlb.objects;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;
import java.io.Serializable;

public record ActiveGame(String gamePk, String lang, String channelId) implements Serializable {
    public String getThreadCode() {
        return "Game-%s-%s".formatted(gamePk, lang);
    }

    public static class EntrySerializer implements Serializer<ActiveGame>, Serializable {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull ActiveGame value) throws IOException {
            out.writeUTF(value.gamePk());
            out.writeUTF(value.lang());
            out.writeUTF(value.channelId());
        }

        @Override
        public ActiveGame deserialize(@NotNull DataInput2 input, int available) throws IOException {
            return new ActiveGame(input.readUTF(), input.readUTF(), input.readUTF());
        }
    }
}
