-- Replace Unsplash placeholders with local uploads for the 12-day Chefchaouen circuit.
UPDATE activities
SET image_url = '/uploads/12-days-casablanca.png',
    updated_at = NOW()
WHERE slug = '12-days-in-morocco-including-chefchaouen';

DELETE FROM activity_gallery_images
WHERE activity_id = (
    SELECT id FROM activities WHERE slug = '12-days-in-morocco-including-chefchaouen'
);

INSERT INTO activity_gallery_images (activity_id, image_url)
SELECT a.id, g.image_url
FROM activities a
CROSS JOIN (
    VALUES
        ('/uploads/12-days-chefchaouen.png'),
        ('/uploads/12-days-fes.png'),
        ('/uploads/12-days-fes-2.png'),
        ('/uploads/12-days-sahara-1.jpeg'),
        ('/uploads/12-days-sahara-2.jpeg'),
        ('/uploads/12-days-sahara-3.jpeg'),
        ('/uploads/12-days-todgha-gorge.png'),
        ('/uploads/12-days-dades-gorge.jpg'),
        ('/uploads/12-days-ait-ben-haddou.png'),
        ('/uploads/12-days-essaouira.png'),
        ('/uploads/12-days-marrakech.png'),
        ('/uploads/12-days-marrakech-2.png')
) AS g(image_url)
WHERE a.slug = '12-days-in-morocco-including-chefchaouen';
