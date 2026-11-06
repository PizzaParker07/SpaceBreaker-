package com.pizzaparker07.spacebreaker.content.research;

import com.pizzaparker07.spacebreaker.core.lang.Lang;

/** Every research string (items, lab GUI, research screen, messages, commands), English + Turkish. */
final class ResearchLang {
    static void add(Lang l) {
        l.add("itemGroup.spacebreaker.research", "Spacebreaker: Research", "Spacebreaker: Araştırma");
        // items
        l.add("item.spacebreaker.tech_disk", "Tech Disk", "Teknoloji Diski");
        l.add("item.spacebreaker.tech_disk.blank", "Blank Tech Disk", "Boş Teknoloji Diski");
        l.add("item.spacebreaker.tech_disk.named", "Tech Disk: %s", "Teknoloji Diski: %s");
        l.add("item.spacebreaker.field_sample", "Field Sample", "Saha Numunesi");
        l.add("item.spacebreaker.field_sample.named", "Field Sample (%s)", "Saha Numunesi (%s)");
        l.add("item.spacebreaker.experiment_data", "Experiment Record", "Deney Kaydı");
        l.add("item.spacebreaker.experiment_data.named", "Experiment Record (%s)", "Deney Kaydı (%s)");
        l.add("item.spacebreaker.sample_kit", "Sample Kit", "Numune Çantası");
        l.add("item.spacebreaker.guide_book", "Spacebreaker Field Guide", "Spacebreaker Saha Rehberi");
        l.add("block.spacebreaker.research_lab", "Research Lab", "Araştırma Laboratuvarı");
        l.add("tooltip.spacebreaker.tech_disk.hint", "Analyse in a Research Lab to learn it", "Öğrenmek için Araştırma Laboratuvarı'nda incele");
        l.add("tooltip.spacebreaker.tech_disk.known", "Already known", "Zaten biliniyor");
        l.add("tooltip.spacebreaker.sample.hint", "Analyse in a Research Lab: may reveal a technology",
                "Araştırma Laboratuvarı'nda incele: bir teknolojiyi ortaya çıkarabilir");
        l.add("tooltip.spacebreaker.sample_kit", "Use on natural ground to take a field sample", "Saha numunesi almak için doğal zemine kullan");
        l.add("tooltip.spacebreaker.guide_book", "Everything you need to know, illustrated", "Bilmen gereken her şey, resimli");
        l.add("tooltip.spacebreaker.research_lab", "Analyses tech disks and samples. Needs FE power.",
                "Teknoloji disklerini ve numuneleri inceler. FE enerjisi gerekir.");
        // sample kinds
        l.add("sample.spacebreaker.earth", "Earth", "Dünya");
        l.add("sample.spacebreaker.moon", "Moon", "Ay");
        l.add("sample.spacebreaker.mars", "Mars", "Mars");
        l.add("sample.spacebreaker.venus", "Venus", "Venüs");
        l.add("sample.spacebreaker.sun", "Sun", "Güneş");
        l.add("sample.spacebreaker.mercury", "Mercury", "Merkür");
        l.add("sample.spacebreaker.phobos", "Phobos", "Phobos");
        l.add("sample.spacebreaker.deimos", "Deimos", "Deimos");
        l.add("sample.spacebreaker.ceres", "Ceres", "Ceres");
        l.add("sample.spacebreaker.vesta", "Vesta", "Vesta");
        l.add("sample.spacebreaker.io", "Io", "Io");
        l.add("sample.spacebreaker.europa", "Europa", "Europa");
        l.add("sample.spacebreaker.ganymede", "Ganymede", "Ganymede");
        l.add("sample.spacebreaker.callisto", "Callisto", "Kallisto");
        l.add("sample.spacebreaker.titan", "Titan", "Titan");
        l.add("sample.spacebreaker.enceladus", "Enceladus", "Enceladus");
        l.add("sample.spacebreaker.triton", "Triton", "Triton");
        l.add("sample.spacebreaker.pluto", "Pluto", "Plüton");
        l.add("sample.spacebreaker.charon", "Charon", "Charon");
        l.add("sample.spacebreaker.meteorite", "Meteorite", "Göktaşı");
        l.add("sample.spacebreaker.suborbital_flight", "Suborbital Flight", "Yörünge Altı Uçuş");
        l.add("sample.spacebreaker.orbital_flight", "Orbital Flight", "Yörünge Uçuşu");
        // lab GUI
        l.add("container.spacebreaker.research_lab", "Research Lab", "Araştırma Laboratuvarı");
        l.add("gui.spacebreaker.lab.researcher", "Researcher: %s", "Araştırmacı: %s");
        l.add("gui.spacebreaker.lab.analysing", "Analysing: %s", "İnceleniyor: %s");
        l.add("gui.spacebreaker.lab.status.0", "Insert a tech disk or sample", "Bir teknoloji diski ya da numune koy");
        l.add("gui.spacebreaker.lab.status.1", "Analysing...", "İnceleniyor...");
        l.add("gui.spacebreaker.lab.status.2", "Not enough power", "Yetersiz enerji");
        l.add("gui.spacebreaker.lab.status.3", "Nothing new in this sample", "Bu numunede yeni bir şey yok");
        l.add("gui.spacebreaker.lab.status.4", "Take the result out first", "Önce sonucu dışarı al");
        l.add("gui.spacebreaker.lab.status.5", "Unreadable data", "Okunamayan veri");
        l.add("gui.spacebreaker.lab.status.6", "Researcher is offline", "Araştırmacı çevrimdışı");
        l.add("gui.spacebreaker.lab.status.7", "Learned: %s", "Öğrenildi: %s");
        l.add("gui.spacebreaker.lab.status.8", "Already known: %s", "Zaten biliniyor: %s");
        l.add("gui.spacebreaker.lab.status.9", "Learn the prerequisites first", "Önce ön koşulları öğren");
        l.add("gui.spacebreaker.lab.energy", "%s / %s FE", "%s / %s FE");
        l.add("gui.spacebreaker.lab.draw", "Uses %s FE/t", "%s FE/t harcar");
        l.add("gui.spacebreaker.lab.time", "%s s left", "%s sn kaldı");
        l.add("gui.spacebreaker.lab.share.on", "Team sharing: on", "Takımla paylaşım: açık");
        l.add("gui.spacebreaker.lab.share.off", "Team sharing: off", "Takımla paylaşım: kapalı");
        l.add("gui.spacebreaker.lab.share.tip", "When on, everyone in your scoreboard team learns what this lab finds.",
                "Açıkken, skor tablosu takımındaki herkes bu laboratuvarın bulduklarını öğrenir.");
        l.add("gui.spacebreaker.lab.tree", "Research tree", "Araştırma ağacı");
        // research screen
        l.add("gui.spacebreaker.research.title", "Research", "Araştırma");
        l.add("gui.spacebreaker.research.progress", "%s of %s technologies", "%s / %s teknoloji");
        l.add("gui.spacebreaker.research.known", "Known", "Biliniyor");
        l.add("gui.spacebreaker.research.available", "Ready to research", "Araştırmaya hazır");
        l.add("gui.spacebreaker.research.locked", "Locked", "Kilitli");
        l.add("gui.spacebreaker.research.tier", "Phase %s", "Faz %s");
        l.add("gui.spacebreaker.research.requires", "Requires:", "Gerekenler:");
        l.add("gui.spacebreaker.research.sources", "Learn from: tech disk", "Öğrenme yolu: teknoloji diski");
        l.add("gui.spacebreaker.research.sources_samples", "Learn from: tech disk, or analysing %s",
                "Öğrenme yolu: teknoloji diski ya da %s incelemesi");
        l.add("gui.spacebreaker.research.cost", "%s s, %s FE/t in a lab", "Laboratuvarda %s sn, %s FE/t");
        l.add("gui.spacebreaker.research.hint", "Drag to move · scroll to zoom", "Taşımak için sürükle · yakınlaştırmak için kaydır");
        l.add("key.spacebreaker.research", "Open research tree", "Araştırma ağacını aç");
        l.add("key.spacebreaker.guide", "Open field guide", "Saha rehberini aç");
        l.add("key.categories.spacebreaker", "Spacebreaker", "Spacebreaker");
        l.add("toast.spacebreaker.tech_learned", "Technology learned!", "Teknoloji öğrenildi!");
        // guide
        l.add("gui.spacebreaker.guide.title", "Field Guide", "Saha Rehberi");
        l.add("gui.spacebreaker.guide.search", "Search...", "Ara...");
        l.add("gui.spacebreaker.guide.results", "Search results", "Arama sonuçları");
        l.add("gui.spacebreaker.guide.none", "Nothing found", "Hiçbir şey bulunamadı");
        l.add("gui.spacebreaker.guide.back", "Back", "Geri");
        l.add("gui.spacebreaker.guide.forward", "Forward", "İleri");
        l.add("gui.spacebreaker.guide.contents", "Contents", "İçindekiler");
        l.add("gui.spacebreaker.guide.intro", "Click a chapter to open it. Arrows or the mouse wheel turn pages; the ribbons on the right jump between topics.",
                "Açmak için bir bölüme tıkla. Sayfaları oklarla ya da fare tekerleğiyle çevir; sağdaki şeritler konular arasında atlar.");
        l.add("gui.spacebreaker.guide.research_needed", "Research needed: %s", "Gereken araştırma: %s");
        l.add("gui.spacebreaker.guide.recipe_info", "%s FE/t · %s s", "%s FE/t · %s sn");
        l.add("gui.spacebreaker.guide.recipe_time", "%s s", "%s sn");
        l.add("gui.spacebreaker.guide.no_recipe", "Recipe %s not found", "%s tarifi bulunamadı");
        // messages
        l.add("message.spacebreaker.tech_learned", "You learned a new technology: %s", "Yeni bir teknoloji öğrendin: %s");
        l.add("message.spacebreaker.requires_tech", "Requires technology: %s", "Gereken teknoloji: %s");
        l.add("message.spacebreaker.sample_taken", "Took a field sample (%s)", "Saha numunesi alındı (%s)");
        // commands
        l.add("commands.spacebreaker.research.unknown", "Unknown technology %s", "Bilinmeyen teknoloji %s");
        l.add("commands.spacebreaker.research.learned", "Taught %s to %s player(s)", "%s, %s oyuncuya öğretildi");
        l.add("commands.spacebreaker.research.forgot", "Removed %s from %s player(s)", "%s, %s oyuncudan kaldırıldı");
        l.add("commands.spacebreaker.research.learned_all", "Taught %s technologies", "%s teknoloji öğretildi");
        l.add("commands.spacebreaker.research.forgot_all", "Removed %s technologies", "%s teknoloji kaldırıldı");
        l.add("commands.spacebreaker.research.list", "%s knows %s of %s technologies:", "%s, %s / %s teknolojiyi biliyor:");
    }

    private ResearchLang() {
    }
}
