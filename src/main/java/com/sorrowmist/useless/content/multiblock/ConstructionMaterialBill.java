package com.sorrowmist.useless.content.multiblock;

import java.util.HashSet;
import java.util.List;

/** A construction checklist, deliberately independent of a nine-slot crafting recipe. */
public record ConstructionMaterialBill(int tier,List<Entry> entries) {
    public record Entry(String item,int count) {
        public Entry {
            if(item==null||!item.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")||count<=0)
                throw new IllegalArgumentException("Invalid construction material");
        }
    }
    public ConstructionMaterialBill {
        if(tier<1||tier>10||entries==null||entries.isEmpty()||entries.size()>81)
            throw new IllegalArgumentException("Invalid construction bill");
        entries=List.copyOf(entries);
        var ids=new HashSet<String>();int total=0;
        for(var e:entries) {
            if(!ids.add(e.item()))throw new IllegalArgumentException("Duplicate material");
            total=Math.addExact(total,e.count());
        }
    }
    public int total(){return entries.stream().mapToInt(Entry::count).sum();}
}
