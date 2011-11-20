-- takes an argument like 10 or 20 to increase the volume

on run argv
  set increase to item 1 of argv
  set current_volume to output volume of (get volume settings)

  if current_volume is less than 100 then
    set current_volume to current_volume + increase
  end if
  set volume output volume current_volume

  say "increased volume"
end run


