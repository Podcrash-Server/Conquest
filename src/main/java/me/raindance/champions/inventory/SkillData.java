package me.raindance.champions.inventory;

import com.podcrash.api.mc.Configurator;
import com.podcrash.api.mc.util.ChatUtil;
import com.podcrash.api.plugin.Pluginizer;
import me.raindance.champions.kits.Skill;
import me.raindance.champions.kits.enums.InvType;
import me.raindance.champions.kits.enums.SkillType;
import org.bukkit.ChatColor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SkillData {
    private final int id;
    private final String name;
    private final InvType invType;
    private final SkillType skillType;
    private final double price;
    private final Constructor<Skill> constructor;

    private List<String> description;

    public SkillData(Skill skill, int id, String name, InvType invType, SkillType skillType, double price) {
        this.id = id;
        this.name = name;
        this.invType = invType;
        this.skillType = skillType;
        //TODO make this not a constant 1000
        this.price = 1000;
        this.constructor = initConstructor(skill);
    }

    private String getCleanName() {
        return getName().toLowerCase().replaceAll("[^A-Za-z0-9]", "").replace(" ", "");
    }
    public CompletableFuture<Void> requestDescription() {
        /*
        String cache = Communicator.getCacheValue(getCleanName());
        if(cache == null) {
            DescriptorTable table = TableOrganizer.getTable(DataTableType.DESCRIPTIONS);
            if(table == null) {
                this.description = Arrays.asList("Error loading skill descriptions!", "null");
                return;
            }
            table.requestCache(getCleanName());
            String value = table.getValue(getCleanName());
            if(value == null || value.isEmpty())
                this.description = Arrays.asList("Error loading skill descriptions!", "null");
            else this.description = Arrays.asList(value.split("\n"));
        }else this.description = Arrays.asList(cache.split("\n"));
         */
        if(description != null && description.size() != 0) return CompletableFuture.completedFuture(null);
        Configurator configurator = Pluginizer.getSpigotPlugin().getConfigurator("skilldescriptions");
        CompletableFuture<Void> future = new CompletableFuture<>();
        configurator.readList(getCleanName(), list -> {
            if(list == null) return;
            if(list.size() != 0) {
                List<String> desc = new ArrayList<>();
                for(Object objLine : list) {
                    if(objLine == null)  {
                        desc.add(null);
                    }else {
                        String line = objLine.toString();
                        line = ChatColor.RESET + ChatUtil.chat(line);
                        desc.add(line);
                    }
                }
                this.description = desc;
            }
            future.complete(null);
        });

        return future;
    }

    private Constructor<Skill> initConstructor(Skill skill) {
        try {
            return (Constructor<Skill>) skill.getClass().getConstructor((Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Skill newInstance() {
        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return description;
    }

    public InvType getInvType() {
        return invType;
    }

    public SkillType getSkillType() {
        return skillType;
    }

    public double getPrice() {return price;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillData that = (SkillData) o;
        return Objects.equals(name, that.name) &&
                invType == that.invType &&
                skillType == that.skillType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, invType, skillType);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SkillData{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", description=").append(description);
        sb.append(", invType=").append(invType);
        sb.append(", skillType=").append(skillType);
        sb.append('}');
        return sb.toString();
    }
}
