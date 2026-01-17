package com.skyblockflipper.backend.NEU.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.skyblockflipper.backend.model.Flipping.Recipe.Recipe;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Item {

    @Id
    @Column(name = "item_id", nullable = false, updatable = false)
    private String id;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "minecraft_id")
    private String minecraftId;

    private String rarity;

    private String category;

    @Lob
    private String lore;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "item_info_links", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "info_link", nullable = false)
    @OrderColumn(name = "position")
    private List<String> infoLinks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "outputItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recipe> recipes = new ArrayList<>();
}
