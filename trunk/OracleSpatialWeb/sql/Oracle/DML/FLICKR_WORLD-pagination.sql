create table FLICKR_WORLD_50M3 as
select *
  from (select t1.*, rownum rn
          from (select p.* from FLICKR_WORLD_50M p) t1
         where rownum <= 15000000)
 where rn > 10000000;
 commit;
